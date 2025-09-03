package org.example.pa;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallerSiteContext;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.MutableSparseIntSet;

public class CustomContextSelector implements ContextSelector {

    private final ClassHierarchy cha;

    public CustomContextSelector(ClassHierarchy cha) {
        this.cha = cha;
    }

    public Context getPredicatedTarget(MethodReference method, IClass declaringClass, Context callerContext) {
        return callerContext;
    }

    public boolean shouldContextBeConsidered(ContextKey key, IMethod method, TypeReference declaredType) {
        return true;
    }

    @Override
    public Context getCalleeTarget(CGNode cgNode, CallSiteReference callSiteReference, IMethod iMethod,
            InstanceKey[] instanceKeys) {

        return null;
    }

    @Override
    public IntSet getRelevantParameters(CGNode cgNode, CallSiteReference callSiteReference) {

        MutableIntSet parameters = MutableSparseIntSet.makeEmpty();
        parameters.add(0);
        return parameters;
    }
}
