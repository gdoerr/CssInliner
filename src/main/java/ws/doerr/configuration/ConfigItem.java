/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2016 Greg Doerr
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package ws.doerr.configuration;

import com.google.common.base.Joiner;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author greg
 */
class ConfigItem {
    private static final Logger LOG = Logger.getLogger(ConfigItem.class.getName());

    protected final String shortName;
    protected final String longName;
    protected final String configName;
    protected final int argCount;
    protected final boolean afterArg;
    private final String defaultValue;

    private final Object container;
    private final Field field;

    private ConfigSource source;
    protected boolean modified = false;

    ConfigItem(ConfigElement element, Object container, Field field) {
        shortName = element.shortName();
        longName = element.longName();
        argCount = element.argCount();
        afterArg = element.afterArg();

        configName = element.configName();

        defaultValue = element.defaultValue();

        this.container = container;
        this.field = field;

        // Make the field accessible in case it's marked private
        field.setAccessible(true);

        if(!defaultValue.isEmpty())
            set(defaultValue, true, ConfigSource.DEFAULT);
    }

    String getName() {
        return MessageFormat.format("{0}.{1}",
            container.getClass().getName(),
            field.getName()
        );
    }

    ConfigSource getSource() {
        return source;
    }

    boolean isName(String name) {
        return field.getName().equals(name);
    }

    final void set(String value, boolean splitIfCollection, ConfigSource source) {
        this.source = source;
        try {
            if(field.getType().isPrimitive()) {
                //boolean, byte, char, short, int, long, float, and double
                switch(field.getGenericType().getTypeName()) {
                    case "boolean":
                        field.setBoolean(container, Boolean.valueOf(value));
                        break;

                    case "byte":
                        field.setByte(container, Byte.valueOf(value));
                        break;

                    case "char":
                        field.setChar(container, value.charAt(0));
                        break;

                    case "short":
                        field.setShort(container, Short.valueOf(value));
                        break;

                    case "int":
                        field.setInt(container, Integer.valueOf(value));
                        break;

                    case "long":
                        field.setLong(container, Long.valueOf(value));
                        break;

                    case "float":
                        field.setFloat(container, Float.valueOf(value));
                        break;

                    case "double":
                        field.setDouble(container, Double.valueOf(value));
                        break;
                }
            } else if(field.getType().isAssignableFrom(String.class)) {
                field.set(container, value);
            } else if(Collection.class.isAssignableFrom(field.getType())) {
                @SuppressWarnings("unchecked")
                Collection<String> f = (Collection<String>) field.get(container);

                if(splitIfCollection) {
                    for(String v : value.split(","))
                        f.add(v);
                } else
                    f.add(value);
            }
        } catch(IllegalAccessException ex) {
            String msg = MessageFormat.format("Exception setting value for {0}.{1}", container.getClass().getName(), field.getName());
            LOG.log(Level.WARNING, msg, ex);
        }
    }

    final String get() {
        try {
            if(field.getType().isPrimitive()) {
                //boolean, byte, char, short, int, long, float, and double
                switch(field.getGenericType().getTypeName()) {
                    case "boolean":
                        return Boolean.toString(field.getBoolean(container));

                    case "byte":
                        return Byte.toString(field.getByte(container));

                    case "char":
                        return Character.toString(field.getChar(container));

                    case "short":
                        return Short.toString(field.getShort(container));

                    case "int":
                        return Integer.toString(field.getInt(container));

                    case "long":
                        return Long.toString(field.getLong(container));

                    case "float":
                        return Float.toString(field.getFloat(container));

                    case "double":
                        return Double.toString(field.getDouble(container));
                }
            } else if(field.getType().isAssignableFrom(String.class)) {
                return (String)field.get(container);
            } else if(Collection.class.isAssignableFrom(field.getType())) {
                @SuppressWarnings("unchecked")
                Collection<String> f = (Collection<String>) field.get(container);
                return Joiner.on(',').join(f);
            }
        } catch(IllegalAccessException ex) {
            String msg = MessageFormat.format("Exception setting value for {0}.{1}", container.getClass().getName(), field.getName());
            LOG.log(Level.WARNING, msg, ex);
        }
        return "";
    }
}
