/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.itsaky.androidide.inflater.internal

import android.view.View
import com.itsaky.androidide.inflater.IAttribute
import com.itsaky.androidide.inflater.IView
import com.itsaky.androidide.inflater.internal.utils.simpleName
import com.itsaky.androidide.inflater.internal.utils.tagName

abstract class AbstractView(
  override val name: String,
  override val view: View,
  override val simpleName: String = name.simpleName(),
  override val tag: String = name.tagName()
) : IView {
  protected val attributes = mutableListOf<IAttribute>()
  
  override fun addAttribute(attribute: IAttribute) {
  
  }
}