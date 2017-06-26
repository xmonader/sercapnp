package com.sercapnp;

/**
 * Created by striky on 6/26/17.
 */

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.*;

public class CapnpElementType extends IElementType {
    public CapnpElementType(@NotNull @NonNls String debugName) {
        super(debugName, CapnpLanguage.INSTANCE);
    }
}

