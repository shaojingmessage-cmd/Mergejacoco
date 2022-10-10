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
package org.jacoco.core.tools;

import org.objectweb.asm.Type;

import java.util.List;

/**
 * @author wl
 */
public class MethodUriAdapter {

	/**
	 * 参数匹配，方法实现来自
	 * https://gitee.com/Dray/jacoco/blob/master/org.jacoco.core/src/org/jacoco/core/internal/diff/CodeDiffUtil.java
	 *
	 * @param params
	 * @param desc
	 * @return
	 */
	public static boolean checkParamsIn(List<String> params, String desc) {
		// 解析ASM获取的参数
		Type[] argumentTypes = Type.getArgumentTypes(desc);
		// 说明是无参数的方法，匹配成功
		if (params.size() == 0 && argumentTypes.length == 0) {
			return Boolean.TRUE;
		}
		// 只有参数数量完全相等才做下一次比较，Type格式：I C Ljava/lang/String;
		if (params.size() > 0 && argumentTypes.length == params.size()) {
			for (int i = 0; i < argumentTypes.length; i++) {
				// 去掉包名只保留最后一位匹配,getClassName格式： int java/lang/String
				String[] args = argumentTypes[i].getClassName().split("\\.");
				String arg = args[args.length - 1];
				// 如果参数是内部类类型，再截取下
				if (arg.contains("$")) {
					arg = arg.split("\\$")[arg.split("\\$").length - 1];
				}
				if (!params.get(i).contains(arg)) {
					return Boolean.FALSE;
				}
			}
			// 只有个数和类型全匹配到才算匹配
			return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}
}
