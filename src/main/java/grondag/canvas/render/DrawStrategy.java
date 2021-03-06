/*******************************************************************************
 * Copyright 2020 grondag
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package grondag.canvas.render;

// TODO: implement draw strategies
public enum DrawStrategy {
	/**
	 * Each shader or conditional has a separate draw call and (if necessary) program activation.
	 * Can only be used for non-translucent.
	 */
	MULTI_DRAW_MULTI_PROGRAM,

	/**
	 * Ubershader controlled by a uniform. Uniform is set before each draw call. Probably most optimal.
	 * Can only be used for non-translucent.
	 */
	MULTI_DRAW_SINGLE_PROGRAM,

	/**
	 * Ubershader controlled by vertex attributes. Requires extra vertex data and shader performance may suffer.
	 * Only option for mixed translucency that doesn't require frequent resorting or exotic approaches.
	 */
	SINGLE_DRAW_MULTI_PROGRAM
}
