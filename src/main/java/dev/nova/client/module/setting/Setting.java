package dev.nova.client.module.setting;

public abstract class Setting<T> {
    private final String name;
    private final String description;
    protected T value;
    protected final T defaultValue;

    public Setting(String name, String description, T defaultValue) {
        this.name = name; this.description = description;
        this.value = defaultValue; this.defaultValue = defaultValue;
    }

    public String getName()     { return name; }
    public String getDescription() { return description; }
    public T      getValue()    { return value; }
    public T      getDefault()  { return defaultValue; }
    public void   setValue(T v) { this.value = v; }
    public void   reset()       { this.value = defaultValue; }

    public static final class BoolSetting extends Setting<Boolean> {
        public BoolSetting(String name, String desc, boolean def) { super(name, desc, def); }
        public void toggle() { value = !value; }
    }

    public static final class NumberSetting extends Setting<Double> {
        private final double min, max, increment;
        public NumberSetting(String name, String desc, double def, double min, double max, double inc) {
            super(name, desc, def);
            this.min = min; this.max = max; this.increment = inc;
        }
        @Override
        public void setValue(Double v) {
            double prec = 1.0 / increment;
            this.value = Math.max(min, Math.min(max, Math.round(v * prec) / prec));
        }
        public double getMin()     { return min; }
        public double getMax()     { return max; }
        public double getInc()     { return increment; }
        // ── typed getters so casts work cleanly ───────────────────────────────
        public int    intValue()   { return (int) Math.round(value); }
        public float  floatValue() { return (float)(double) value; }
        public long   longValue()  { return Math.round(value); }
        public double doubleValue(){ return value; }
    }

    public static final class ModeSetting<E extends Enum<E>> extends Setting<E> {
        private final E[] values;
        @SuppressWarnings("unchecked")
        public ModeSetting(String name, String desc, E def) {
            super(name, desc, def);
            this.values = (E[]) def.getClass().getEnumConstants();
        }
        public void cycle() { value = values[(value.ordinal() + 1) % values.length]; }
        public E[] getValues() { return values; }
    }
}
