// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

/**
 * Setting containing a {@link String} value.
 * @since 9759
 */
public class StringSetting extends AbstractSetting<String> {
    /**
     * Constructs a new {@code StringSetting} with the given value
     * @param value The setting value
     */
    public StringSetting(String value) {
        super(value);
    }

    @Override
    public boolean equalVal(String otherVal) {
        if (value == null)
            return otherVal == null;
        return value.equals(otherVal);
    }

    @Override
    public StringSetting copy() {
        return new StringSetting(value);
    }

    @Override
    public void visit(SettingVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public StringSetting getNullInstance() {
        return new StringSetting(null);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof StringSetting))
            return false;
        return equalVal(((StringSetting) other).getValue());
    }
}
