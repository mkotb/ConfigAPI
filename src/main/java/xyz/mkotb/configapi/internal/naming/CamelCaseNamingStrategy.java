/*
 * Copyright (c) 2016, Mazen Kotb, mazenkotb@gmail.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package xyz.mkotb.configapi.internal.naming;

import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CamelCaseNamingStrategy implements NamingStrategy {
    private static final Pattern CAMEL_PATTERN = Pattern.compile("(?<=[^_\\r\\n])(?:(?>_+)|(?=[A-Z])(?:(?<![A-Z])|(?=[A-Z][a-z]))" +
            "|(?>(?=\\d))(?<!\\d)|(?<=\\d)(?=[^\\d\\r\\n]))");

    @Override
    public String rename(String input) {
        return String.join("-", Stream.of(CAMEL_PATTERN.split(input))
                .map(String::toLowerCase).collect(Collectors.toList()));
    }
}
