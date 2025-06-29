package com.sercapnp.lang;

/**
 * Created by striky on 6/26/17.
 * Updated by meetzli on 6/29/25 for Intellij Platform 2025.1 compatibility
 */

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.*;

public class CapnpElementType extends IElementType {
    public CapnpElementType(@NotNull @NonNls String debugName) {
        super(debugName, CapnpLanguage.INSTANCE);
    }
}