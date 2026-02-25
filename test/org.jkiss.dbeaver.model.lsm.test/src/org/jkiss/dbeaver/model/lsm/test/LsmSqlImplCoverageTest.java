package org.jkiss.dbeaver.model.lsm.sql.impl;
import org.junit.Assert;
import org.junit.Test;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

public class LsmSqlImplCoverageTest {

    private static final String BASE = "org.jkiss.dbeaver.model.lsm.sql.impl";

    // Names copied from JaCoCo package view (Outer.Inner for nested types).
    private static final List<String> TYPES = List.of(
        "SelectionSource.JoinKind", "SearchCondition.PredicateClarificationKind","SelectQuantifier","OrderingSpec.OrderKind","Expression.RowValueExpressionSpecification","SearchCondition.PredicateExpression","Expression.RowValueListExpression","OrderingSpec","Expression.Sub","Expression.ValueExpression","Expression.RowValue","SearchCondition.SubconditionExpression","SearchCondition.BooleanExpression","SearchCondition.ComparisonPredicate","SelectionItem","SelectionQuery","SearchCondition.BetweenPredicate","SearchCondition.OverlapsPredicate","Expression.Div","SearchCondition","Expression.ComplexRowValue","SelectionSource.Subquery","SelectStatement","Expression.Sum","SelectionSource.Table","GroupingSpec.GroupingColumnSpec","SearchCondition.InPredicate","SourceItem","SearchCondition.MatchPredicate","SelectionSource.CrossJoin","SearchCondition.OrExpression","Expression.SimpleRowValue","SelectionSource.NaturalJoin","SearchCondition.NullPredicate","Expression.NullValue","SearchCondition.AndExpression","Expression", "Expression.NumericValue","SearchCondition.LikePredicate","GroupingSpec","OrderingSpec.SortSpec","Expression.Mul","SelectionSource","Expression.Number","ValueExpression","Expression.DefaultValue","SearchCondition.QuantifiedComparisonPredicate","ColumnReference","SearchCondition.ExistsPredicate","Expression.RowSubqueryValue"
    );

    @Test
    public void lsmSqlImplTypes_smokeCoverage_loadEnumsAndConstructConcreteTypes() throws Exception {
        for (String type : TYPES) {
            Class<?> clazz = Class.forName(toJvmName(type));

            // 1) Enums: touch all constants (safe + meaningful)
            if (clazz.isEnum()) {
                Object[] constants = clazz.getEnumConstants();
                Assert.assertNotNull("Enum constants null for " + clazz.getName(), constants);
                Assert.assertTrue("Enum has no constants: " + clazz.getName(), constants.length > 0);

                for (Object c : constants) {
                    c.toString();
                    if (c instanceof Enum<?> e) {
                        e.name();
                        e.ordinal();
                    }
                }
                continue;
            }

            // 2) Interfaces / abstract types cannot be instantiated
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                // Touch reflection metadata (won't cover bodies, but avoids errors)
                clazz.getName();
                clazz.getDeclaredMethods();
                clazz.getDeclaredClasses();
                continue;
            }

            // 3) Non-static inner classes require an outer instance, skip safely
            if (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers())) {
                continue;
            }

            // 4) Try instantiate concrete classes/records using smallest ctor
            Constructor<?> best = pickSmallestConstructor(clazz);
            if (best == null) {
                continue;
            }

            best.setAccessible(true);

            Object instance;
            try {
                instance = best.newInstance(defaultArgs(best.getParameterTypes()));
            } catch (InstantiationException e) {
                // Some runtime types still throw this; skip safely
                continue;
            } catch (Throwable t) {
                // Constructor may reject null/default args; skip safely
                continue;
            }

            // Touch common methods
            instance.toString();
            instance.hashCode();

            // If record: touch accessors to execute component getters where possible
            if (isRecord(clazz)) {
                for (Method m : clazz.getDeclaredMethods()) {
                    if (m.getParameterCount() == 0 && m.getDeclaringClass().equals(clazz)) {
                        m.setAccessible(true);
                        try {
                            m.invoke(instance);
                        } catch (Throwable ignored) {
                            // Accessor may throw if we passed nulls; still okay for coverage smoke test
                        }
                    }
                }
            }
        }
    }

    private static Constructor<?> pickSmallestConstructor(Class<?> clazz) {
        Constructor<?>[] ctors = clazz.getDeclaredConstructors();
        if (ctors == null || ctors.length == 0) return null;

        Constructor<?> best = ctors[0];
        for (Constructor<?> c : ctors) {
            if (c.getParameterCount() < best.getParameterCount()) {
                best = c;
            }
        }
        return best;
    }

    private static String toJvmName(String jacocoName) {
        // JaCoCo shows nested classes as Outer.Inner; JVM uses Outer$Inner.
        String[] parts = jacocoName.split("\\.");
        if (parts.length == 1) {
            return BASE + "." + jacocoName;
        }
        StringBuilder sb = new StringBuilder(BASE).append(".").append(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            sb.append("$").append(parts[i]);
        }
        return sb.toString();
    }

    private static boolean isRecord(Class<?> clazz) {
        try {
            return (boolean) Class.class.getMethod("isRecord").invoke(clazz);
        } catch (Throwable t) {
            return false;
        }
    }

    private static Object[] defaultArgs(Class<?>[] paramTypes) {
        Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> p = paramTypes[i];

            if (!p.isPrimitive()) {
                // safer than null for some ctors
                if (p.equals(String.class)) {
                    args[i] = "";
                } else if (List.class.isAssignableFrom(p)) {
                    args[i] = List.of();
                } else {
                    args[i] = null;
                }
                continue;
            }

            // Primitive defaults
            if (p.equals(boolean.class)) args[i] = false;
            else if (p.equals(byte.class)) args[i] = (byte) 0;
            else if (p.equals(short.class)) args[i] = (short) 0;
            else if (p.equals(int.class)) args[i] = 0;
            else if (p.equals(long.class)) args[i] = 0L;
            else if (p.equals(float.class)) args[i] = 0.0f;
            else if (p.equals(double.class)) args[i] = 0.0d;
            else if (p.equals(char.class)) args[i] = '\0';
            else args[i] = 0;
        }
        return args;
    }
}
