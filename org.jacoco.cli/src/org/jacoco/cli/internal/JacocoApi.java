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
package org.jacoco.cli.internal;

import java.io.PrintWriter;

public class JacocoApi {

	protected static int result;

	public static int execute(String... args) throws Exception {
		final PrintWriter out = new PrintWriter(System.out, true);
		final PrintWriter err = new PrintWriter(System.err, true);
		result = new Main(args).execute(new PrintWriter(out),
				new PrintWriter(err));
		return result;
	}
}
