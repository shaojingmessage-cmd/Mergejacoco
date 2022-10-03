/*******************************************************************************
 * Copyright (c) 2009, 2021 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *
 *******************************************************************************/
package org.jacoco.cli.internal.commands;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jacoco.cli.internal.Command;
import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.core.tools.ExecMergeHandler;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/**
 * The <code>merge</code> command.
 */
public class Merge extends Command {

	@Argument(usage = "list of JaCoCo *.exec files to read", metaVar = "<execfiles>")
	List<File> execfiles = new ArrayList<File>();

	@Option(name = "--destfile", usage = "file to write merged execution data to", metaVar = "<path>", required = true)
	File destfile;

	@Option(name = "--id", usage = "project id Use when merging different versions exec file", metaVar = "<n>")
	int projectId;

	@Option(name = "--diffPort", usage = "code-diff services port", required = true)
	int diffPort;

	@Override
	public String description() {
		return "Merges multiple exec files into a new one.";
	}

	@Override
	public int execute(final PrintWriter out, final PrintWriter err)
			throws IOException {
		final ExecFileLoader loader = loadExecutionData(out);
		out.printf("[INFO] Writing execution data to %s.%n",
				destfile.getAbsolutePath());
		loader.save(destfile, true);
		return 0;
	}

	// private ExecFileLoader loadExecutionData(final PrintWriter out)
	// throws IOException {
	// final ExecFileLoader loader = new ExecFileLoader();
	// if (execfiles.isEmpty()) {
	// out.println("[WARN] No execution data files provided.");
	// } else {
	// for (final File file : execfiles) {
	// out.printf("[INFO] Loading execution data file %s.%n",
	// file.getAbsolutePath());
	// loader.load(file);
	// }
	// }
	// return loader;
	// }
	private ExecFileLoader loadExecutionData(final PrintWriter out)
			throws IOException {
		ExecFileLoader loader = new ExecFileLoader();
		if (execfiles.isEmpty()) {
			out.println("[WARN] No execution data files provided.");
		} else {
			List<ExecFileLoader> execFileLoaders = new ArrayList<ExecFileLoader>();
			for (final File file : execfiles) {
				out.printf("[INFO] Loading execution data file %s.%n",
						file.getAbsolutePath());
				ExecFileLoader subData = new ExecFileLoader();
				subData.load(file);
				execFileLoaders.add(subData);
			}
			execSort(execFileLoaders);
			loader = new ExecMergeHandler(projectId, diffPort)
					.mergeExecHandle(execFileLoaders);
		}
		return loader;
	}

	/**
	 * 按照开始记录的时间戳倒叙list
	 *
	 * @param execFileLoaders
	 */
	private void execSort(List<ExecFileLoader> execFileLoaders) {
		Collections.sort(execFileLoaders, new Comparator<ExecFileLoader>() {
			@Override
			public int compare(ExecFileLoader o1, ExecFileLoader o2) {
				int l1 = o1.getSessionInfoStore().getInfos().size();
				int l2 = o2.getSessionInfoStore().getInfos().size();
				return (int) (o2.getSessionInfoStore().getInfos().get(l2 - 1)
						.getStartTimeStamp()
						- o1.getSessionInfoStore().getInfos().get(l1 - 1)
								.getStartTimeStamp());
			}
		});
	}

}
