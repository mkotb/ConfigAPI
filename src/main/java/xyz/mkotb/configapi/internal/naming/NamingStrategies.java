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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NamingStrategies {
    private static Map<String, NamingStrategy> strategies = new ConcurrentHashMap<>();

    private NamingStrategies() {
        insert("underscore", new UnderscoreNamingStrategy());
        insert("camelcase", new CamelCaseNamingStrategy());
        insert("dummy", new DummyNamingStrategy());
        insert("null", from("dummy"));
    }

    public static NamingStrategy from(String name) {
        return strategies.get(name.toLowerCase());
    }

    public static void insert(String name, NamingStrategy strategy) {
        strategies.replace(name.toLowerCase(), strategy); // guaranteed atomicy
    }
}
