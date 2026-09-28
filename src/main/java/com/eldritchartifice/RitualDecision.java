package com.eldritchartifice;

/** Pure decision rules shared by ritual validation and completion. */
final class RitualDecision {
    enum Outcome { REJECT, BOSS, JOIN, AUDIENCE, ADVANCE }
    record Decision(Outcome outcome,String error,int targetTier) {}
    static Decision decide(int tier,boolean member,boolean unaligned,boolean completed,
                           boolean atBastion,boolean doorwayReady,boolean audienceAvailable) {
        if(tier<2)return new Decision(Outcome.REJECT,"The Gate will not answer before the second tier of study.",tier);
        // Hostile summoning does not join or advance anyone, including foreign factions.
        if(atBastion)return doorwayReady?new Decision(Outcome.BOSS,null,tier):
            new Decision(Outcome.REJECT,"The threshold must be whole and still before it can open again.",tier);
        if(!member&&tier==2&&!completed)return new Decision(Outcome.REJECT,"Complete the studies of the second tier before approaching the Gate.",tier);
        if(!audienceAvailable)return new Decision(Outcome.REJECT,"The attention beyond the veil is elsewhere. Wait a little before calling again.",tier);
        if(!member)return new Decision(Outcome.JOIN,null,tier==2?3:tier);
        if(tier<5&&completed)return new Decision(Outcome.ADVANCE,null,tier+1);
        return new Decision(Outcome.AUDIENCE,null,tier);
    }
}
