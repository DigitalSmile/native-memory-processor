/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.openjdk.jextract;

import org.openjdk.jextract.impl.Parser;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simple extraction tool which generates a minimal Java API. Such an API consists mainly of static methods,
 * where for each native function a static method is added which calls the underlying native method handles.
 * Similarly, for struct fields and global variables, static accessors (getter and setter) are generated
 * on top of the underlying memory access var handles. For each struct, a static layout field is generated.
 */
public final class JextractTool {
    public static final boolean DEBUG = Boolean.getBoolean("jextract.debug");

    /**
     * Parse input files into a toplevel declaration with given options.
     * @return a toplevel declaration.
     */
    public static Declaration.Scoped parse(Path header) {
        //Path source = /*headers.size() > 1? generateTmpSource(headers) : */ headers.iterator().next();
        return new Parser().parse(header, Collections.emptyList());
    }

    public static void main(String[] args) {
        var path = Path.of(args[0]);
    }
}
