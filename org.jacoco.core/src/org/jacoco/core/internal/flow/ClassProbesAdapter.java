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
package org.jacoco.core.internal.flow;

import com.mario.common.repo.enums.ChangeTypeEnum;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.data.MethodProbesInfo;
import org.jacoco.core.internal.analysis.ClassCoverageImpl;
import org.jacoco.core.internal.instr.InstrSupport;
import org.jacoco.core.tools.MethodUriAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AnalyzerAdapter;

import java.util.Objects;

/**
 * A {@link org.objectweb.asm.ClassVisitor} that calculates probes for every
 * method.
 */
public class ClassProbesAdapter extends ClassVisitor
		implements IProbeIdGenerator {

	private static final MethodProbesVisitor EMPTY_METHOD_PROBES_VISITOR = new MethodProbesVisitor() {
	};

	private final ClassProbesVisitor cv;

	private final boolean trackFrames;

	private int counter = 0;

	private String name;

	private ClassCoverageImpl coverage;

	/**
	 * Creates a new adapter that delegates to the given visitor.
	 *
	 * @param cv
	 *            instance to delegate to
	 * @param trackFrames
	 *            if <code>true</code> stackmap frames are tracked and provided
	 */
	public ClassProbesAdapter(final ClassProbesVisitor cv,
			final boolean trackFrames) {
		super(InstrSupport.ASM_API_VERSION, cv);
		this.cv = cv;
		this.trackFrames = trackFrames;
	}

	public ClassProbesAdapter(final ClassProbesVisitor cv,
			final boolean trackFrames, ClassCoverageImpl coverage) {
		super(InstrSupport.ASM_API_VERSION, cv);
		this.cv = cv;
		this.trackFrames = trackFrames;
		this.coverage = coverage;
	}

	@Override
	public void visit(final int version, final int access, final String name,
			final String signature, final String superName,
			final String[] interfaces) {
		this.name = name;
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public final MethodVisitor visitMethod(final int access, final String name,
			final String desc, final String signature,
			final String[] exceptions) {
		final MethodProbesVisitor methodProbes;
		final MethodProbesVisitor mv = cv.visitMethod(access, name, desc,
				signature, exceptions);
		if (mv == null) {
			// We need to visit the method in any case, otherwise probe ids
			// are not reproducible
			methodProbes = EMPTY_METHOD_PROBES_VISITOR;
		} else {
			if (this.coverage != null) {
				MethodProbesInfo info = new MethodProbesInfo();
				info.setMethodName(name);
				info.setStartIndex(counter);
				info.setMethodUri(this.name + "." + name + desc);
				info.setDesc(desc);
				this.coverage.getMethodProbesInfos().add(info);
			}
			// 增量覆盖方法过滤;只统计修改过|新增的方法
			if (!Objects.isNull(CoverageBuilder.getDiffList())) {
				boolean flag = CoverageBuilder.getDiffList().stream()
						.filter(classInfo -> classInfo
								.getType() != ChangeTypeEnum.DELETE)
						.filter(classInfo -> this.name
								.equals(classInfo.getClassName()))
						.anyMatch(classInfo -> {
							// 如果是新增类,直接返回true
							if (classInfo.getType() == ChangeTypeEnum.ADD) {
								return true;
							}
							boolean b = classInfo.getMethodInfos().stream()
									// 过滤掉删除的方法
									.filter(methodInfo -> methodInfo
											.getType() != ChangeTypeEnum.DELETE)
									// 过滤掉不是同一方法名
									.filter(methodInfo -> methodInfo
											.getMethodName()
											.equalsIgnoreCase(name))
									// 检查参数是否一致
									.anyMatch(methodInfo -> MethodUriAdapter
											.checkParamsIn(
													methodInfo.getParameters(),
													desc));
							return b;
						});
				if (flag) {
					methodProbes = mv;
				}
				// 增量覆盖，方法不是新增|修改 过滤掉
				else {
					methodProbes = EMPTY_METHOD_PROBES_VISITOR;
				}
			}
			// 全量或者切割方法探针时
			else {
				methodProbes = mv;
			}
			// methodProbes = mv;
		}
		return new MethodSanitizer(null, access, name, desc, signature,
				exceptions) {

			@Override
			public void visitEnd() {
				super.visitEnd();
				LabelFlowAnalyzer.markLabels(this);
				final MethodProbesAdapter probesAdapter = new MethodProbesAdapter(
						methodProbes, ClassProbesAdapter.this, coverage);
				if (trackFrames) {
					final AnalyzerAdapter analyzer = new AnalyzerAdapter(
							ClassProbesAdapter.this.name, access, name, desc,
							probesAdapter);
					probesAdapter.setAnalyzer(analyzer);
					methodProbes.accept(this, analyzer);
				} else {
					methodProbes.accept(this, probesAdapter);
				}
			}
		};
	}

	@Override
	public void visitEnd() {
		cv.visitTotalProbeCount(counter);
		super.visitEnd();
	}

	// === IProbeIdGenerator ===

	@Override
	public int nextId() {
		return counter++;
	}

	@Override
	public int getCurrentId() {
		return counter - 1 > -1 ? counter - 1 : -1;
	}

}
