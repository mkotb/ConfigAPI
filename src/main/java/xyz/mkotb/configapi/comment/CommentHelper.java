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
package xyz.mkotb.configapi.comment;

import xyz.mkotb.configapi.internal.naming.NamingStrategy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public final class CommentHelper {
    private CommentHelper() {
    }

    public static void encodeComments(String[] comments, StringBuilder builder) {
        for (String comment : comments) {
            builder.append("# ").append(comment).append("\n");
        }
    }

    public static Map<String, String[]> extractComments(Object object, NamingStrategy namingStrat) {
        Map<String, String[]> comments = new HashMap<>();

        for(Field field : object.getClass().getDeclaredFields()) {
            for (Annotation annotation : field.getDeclaredAnnotations()) {
                String[] value = valueFrom(annotation);

                if (value != null) {
                    comments.put(namingStrat.rename(field.getName()), value);
                    break;
                }
            }
        }

        return comments;
    }

    public static String[] extractHeader(Class<?> cls) {
        for (Annotation annotation : cls.getAnnotations()) {
            String[] value = valueFrom(annotation);

            if (value != null) {
                return value;
            }
        }

        return null;
    }

    public static String[] valueFrom(Annotation annotation) {
        if (annotation instanceof Comment) {
            return new String[] {((Comment) annotation).value()};
        }

        if (annotation instanceof HeaderComment) {
            return new String[] {((HeaderComment) annotation).value()};
        }

        if (annotation instanceof Comments) {
            return ((Comments) annotation).value();
        }

        if (annotation instanceof HeaderComments) {
            return ((HeaderComments) annotation).value();
        }

        return null;
    }
}
