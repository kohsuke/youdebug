package org.kohsuke.autodbg;

import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.ByteValue;
import com.sun.jdi.CharValue;
import com.sun.jdi.ShortValue;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.LongValue;
import com.sun.jdi.FloatValue;
import com.sun.jdi.DoubleValue;

/**
 * Variable that supports get/set.
 *
 * @author Kohsuke Kawaguchi
 */
abstract class Variable {
    public void set(Object o) {
        set(wrap(vm(),o));
    }

    protected abstract VirtualMachine vm();
    public abstract void set(Value o);
    public abstract Value get();
    public Object getUnwrapped() {
        return unwrap(get());
    }

    /**
     * Wraps a value into a JDI-compatible {@link Value}.
     */
    static Value wrap(VirtualMachine vm, Object o) {
        if (o==null)    return null;
        if (o instanceof Value)
            return (Value) o;
        if (o instanceof Boolean)
            return vm.mirrorOf((Boolean) o);
        if (o instanceof Byte)
            return vm.mirrorOf((Byte) o);
        if (o instanceof Character)
            return vm.mirrorOf((Character) o);
        if (o instanceof Short)
            return vm.mirrorOf((Short) o);
        if (o instanceof Integer)
            return vm.mirrorOf((Integer) o);
        if (o instanceof Long)
            return vm.mirrorOf((Long) o);
        if (o instanceof Float)
            return vm.mirrorOf((Float) o);
        if (o instanceof Double)
            return vm.mirrorOf((Double) o);
        if (o instanceof String)
            return vm.mirrorOf((String) o);
        throw new IllegalArgumentException("Don't know how to wrap "+o.getClass());
    }

    static Object unwrap(Value v) {
        if (v instanceof BooleanValue)
            return ((BooleanValue) v).booleanValue();
        if (v instanceof ByteValue)
            return ((ByteValue) v).byteValue();
        if (v instanceof CharValue)
            return ((CharValue) v).charValue();
        if (v instanceof ShortValue)
            return ((ShortValue) v).shortValue();
        if (v instanceof IntegerValue)
            return ((IntegerValue) v).intValue();
        if (v instanceof LongValue)
            return ((LongValue) v).longValue();
        if (v instanceof FloatValue)
            return ((FloatValue) v).floatValue();
        if (v instanceof DoubleValue)
            return ((DoubleValue) v).doubleValue();
        return v;
    }

    static Variable fromMethodArgument(final StackFrame f, final int index) {
        return new Variable() {
            protected VirtualMachine vm() {
                return f.virtualMachine();
            }

            public void set(Value o) {
                throw new UnsupportedOperationException();
            }

            public Value get() {
                return f.getArgumentValues().get(index);
            }
        };
    }

    static Variable fromLocalVariable(final StackFrame f, final LocalVariable v) {
        return new Variable() {
            protected VirtualMachine vm() {
                return f.virtualMachine();
            }

            public void set(Value o) {
                try {
                    f.setValue(v,o);
                } catch (InvalidTypeException e) {
                    throw new FailedAssignmentException(e);
                } catch (ClassNotLoadedException e) {
                    throw new FailedAssignmentException(e);
                }
            }

            public Value get() {
                return f.getValue(v);
            }
        };
    }

    static Variable fromField(final ObjectReference _this, final Field f) {
        return new Variable() {
            protected VirtualMachine vm() {
                return _this.virtualMachine();
            }

            public void set(Value o) {
                try {
                    _this.setValue(f,o);
                } catch (InvalidTypeException e) {
                    throw new FailedAssignmentException(e);
                } catch (ClassNotLoadedException e) {
                    throw new FailedAssignmentException(e);
                }
            }

            public Value get() {
                return _this.getValue(f);
            }
        };
    }

    /**
     * For static fields.
     */
    static Variable fromField(final ReferenceType type, final Field f) {
        return new Variable() {
            protected VirtualMachine vm() {
                return type.virtualMachine();
            }

            public void set(Value o) {
                if (type instanceof ClassType) {
                    ClassType ct = (ClassType) type;
                    try {
                        ct.setValue(f,o);
                    } catch (InvalidTypeException e) {
                        throw new FailedAssignmentException(e);
                    } catch (ClassNotLoadedException e) {
                        throw new FailedAssignmentException(e);
                    }
                } else {
                    throw new FailedAssignmentException("Can't set a field of "+type.name());
                }
            }

            public Value get() {
                return type.getValue(f);
            }
        };
    }
}
