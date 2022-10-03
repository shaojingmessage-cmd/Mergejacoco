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
package org.jacoco.core.analysis;

import java.util.*;

import com.test.diff.common.domain.ClassInfo;
import org.jacoco.core.internal.analysis.BundleCoverageImpl;
import org.jacoco.core.internal.analysis.SourceFileCoverageImpl;
import org.jacoco.core.runtime.WildcardMatcher;

/**
 * Builder for hierarchical {@link ICoverageNode} structures from single
 * {@link IClassCoverage} nodes. The nodes are feed into the builder through its
 * {@link ICoverageVisitor} interface. Afterwards the aggregated data can be
 * obtained with {@link #getClasses()}, {@link #getSourceFiles()} or
 * {@link #getBundle(String)} in the following hierarchy:
 *
 * <pre>
 * {@link IBundleCoverage}
 * +-- {@link IPackageCoverage}*
 *     +-- {@link IClassCoverage}*
 *     +-- {@link ISourceFileCoverage}*
 * </pre>
 */
public class CoverageBuilder implements ICoverageVisitor {

	private final Map<String, IClassCoverage> classes;

	private final Map<String, ISourceFileCoverage> sourcefiles;

	private static final ThreadLocal<List<ClassInfo>> diffLocal = new ThreadLocal<List<ClassInfo>>();

	private static final ThreadLocal<TypeEnum> typeLocal = new ThreadLocal<TypeEnum>();

	/**
	 * 在做全量覆盖率时，会根据这个通配符表达式过滤不需要的类
	 */
	private static final ThreadLocal<List<Map<String, WildcardMatcher>>> filterRulesLocal = new ThreadLocal<List<Map<String, WildcardMatcher>>>();

	/**
	 * Create a new builder.
	 *
	 */
	public CoverageBuilder() {
		this.classes = new HashMap<String, IClassCoverage>();
		this.sourcefiles = new HashMap<String, ISourceFileCoverage>();
	}

	/**
	 * Returns all class nodes currently contained in this builder.
	 *
	 * @return all class nodes
	 */
	public Collection<IClassCoverage> getClasses() {
		return Collections.unmodifiableCollection(classes.values());
	}

	public Map<String, IClassCoverage> getClassesMap() {
		return this.classes;
	}

	/**
	 * Returns all source file nodes currently contained in this builder.
	 *
	 * @return all source file nodes
	 */
	public Collection<ISourceFileCoverage> getSourceFiles() {
		return Collections.unmodifiableCollection(sourcefiles.values());
	}

	/**
	 * Creates a bundle from all nodes currently contained in this bundle.
	 *
	 * @param name
	 *            Name of the bundle
	 * @return bundle containing all classes and source files
	 */
	public IBundleCoverage getBundle(final String name) {
		return new BundleCoverageImpl(name, classes.values(),
				sourcefiles.values());
	}

	/**
	 * Returns all classes for which execution data does not match.
	 *
	 * @see IClassCoverage#isNoMatch()
	 * @return collection of classes with non-matching execution data
	 */
	public Collection<IClassCoverage> getNoMatchClasses() {
		final Collection<IClassCoverage> result = new ArrayList<IClassCoverage>();
		for (final IClassCoverage c : classes.values()) {
			if (c.isNoMatch()) {
				result.add(c);
			}
		}
		return result;
	}

	// === ICoverageVisitor ===

	public void visitCoverage(final IClassCoverage coverage) {
		final String name = coverage.getName();
		final IClassCoverage dup = classes.put(name, coverage);
		if (dup != null) {
			if (dup.getId() != coverage.getId()) {
				throw new IllegalStateException(
						"Can't add different class with same name: " + name);
			}
		} else {
			final String source = coverage.getSourceFileName();
			if (source != null) {
				final SourceFileCoverageImpl sourceFile = getSourceFile(source,
						coverage.getPackageName());
				sourceFile.increment(coverage);
			}
		}
	}

	private SourceFileCoverageImpl getSourceFile(final String filename,
			final String packagename) {
		final String key = packagename + '/' + filename;
		SourceFileCoverageImpl sourcefile = (SourceFileCoverageImpl) sourcefiles
				.get(key);
		if (sourcefile == null) {
			sourcefile = new SourceFileCoverageImpl(filename, packagename);
			sourcefiles.put(key, sourcefile);
		}
		return sourcefile;
	}

	public static void setDiffList(List<ClassInfo> diffList) {
		diffLocal.set(diffList);
	}

	public static List<ClassInfo> getDiffList() {
		return diffLocal.get();
	}

	public static void setFilterRulesLocal(List<String> rules) {
		List<Map<String, WildcardMatcher>> list = new ArrayList<Map<String, WildcardMatcher>>();
		for (int i = 0; i < rules.size(); i++) {
			Map<String, WildcardMatcher> map = new HashMap<String, WildcardMatcher>(
					2);
			WildcardMatcher includes = new WildcardMatcher(
					toVMName(rules.get(i)));
			WildcardMatcher excludes = new WildcardMatcher(
					toVMName(rules.get(++i)));
			map.put("includes", includes);
			map.put("excludes", excludes);
			list.add(map);
		}
		filterRulesLocal.set(list);
	}

	private static String toVMName(final String srcName) {
		return srcName.replace('.', '/');
	}

	public static List<Map<String, WildcardMatcher>> getFilterRulesLocal() {
		return filterRulesLocal.get();
	}

	public static void setType(TypeEnum type) {
		CoverageBuilder.typeLocal.set(type);
	}

	public static TypeEnum getType() {
		return CoverageBuilder.typeLocal.get();
	}

	public enum TypeEnum {
		MERGE(1, "merge"), REPORT(2, "report");
		private int code;
		private String desc;

		TypeEnum(int code, String desc) {
			this.code = code;
			this.desc = desc;
		}
	}

}
