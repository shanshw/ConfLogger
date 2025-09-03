package org.example.pa;

import com.ibm.wala.ssa.*;
import com.ibm.wala.util.collections.Pair;

import java.util.List;

public class logPiNodePolicy implements SSAPiNodePolicy {

    @Override
    public Pair<Integer, SSAInstruction> getPi(SSAAbstractInvokeInstruction ssaAbstractInvokeInstruction,
            SymbolTable symbolTable) {
        System.out.println("getPi in invoke:" + ssaAbstractInvokeInstruction);
        return null;
    }

    @Override
    public Pair<Integer, SSAInstruction> getPi(SSAConditionalBranchInstruction ssaConditionalBranchInstruction,
            SSAInstruction ssaInstruction, SSAInstruction ssaInstruction1, SymbolTable symbolTable) {
        System.out.println("getPi in branch:" + ssaConditionalBranchInstruction);

        return null;
    }

    @Override
    public List<Pair<Integer, SSAInstruction>> getPis(SSAConditionalBranchInstruction ssaConditionalBranchInstruction,
            SSAInstruction ssaInstruction, SSAInstruction ssaInstruction1, SymbolTable symbolTable) {
        System.out.println("getPis in branch:" + ssaConditionalBranchInstruction + "\tget Pis in ins:" + ssaInstruction
                + "\tget Pis in ins1:" + ssaInstruction1);

        return null;
    }
}
