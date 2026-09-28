package com.eldritchartifice;

import com.mna.api.rituals.IRitualContext;
import com.mna.api.capabilities.IPlayerProgression;
import com.mna.capabilities.playerdata.progression.PlayerProgressionProvider;
import com.mna.api.rituals.RitualEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import static com.eldritchartifice.ShoggothService.*;

/** One Gate and Key ritual: initiation, peaceful advancement, or a bastion encounter. */
class RitualEffectOpenEye extends RitualEffect {
    static final ResourceLocation RITUAL_ID=new ResourceLocation(EldritchArtifice.MOD_ID,"rituals/open_eye");
    static final ResourceLocation EFFECT_ID=new ResourceLocation(EldritchArtifice.MOD_ID,"ritual-effect-open-eye");
    RitualEffectOpenEye(){this(RITUAL_ID);}
    protected RitualEffectOpenEye(ResourceLocation ritual){super(ritual);}

    static double[] location(IRitualContext context){
        Object p=context.getCenter();
        return new double[]{((Number)call(p,"getX|m_123341_")).doubleValue()+.5,
            ((Number)call(p,"getY|m_123342_")).doubleValue(),
            ((Number)call(p,"getZ|m_123343_")).doubleValue()+.5};
    }
    private RitualDecision.Decision decision(IRitualContext context){
        Player caster=context.getCaster();
        if(caster==null)return new RitualDecision.Decision(RitualDecision.Outcome.REJECT,"The Gate cannot find its caller.",0);
        IPlayerProgression p=caster.getCapability(PlayerProgressionProvider.PROGRESSION).orElse(null);
        if(p==null)return new RitualDecision.Decision(RitualDecision.Outcome.REJECT,"The Eye cannot yet discern the course of your studies.",0);
        String allied=MnaIntegration.currentAlliedFactionId(caster);
        double[] at=location(context);
        boolean bastion=RiftArena.center!=null&&context.getLevel()==RiftArena.level&&distance(at,RiftArena.center)<=16;
        return RitualDecision.decide(p.getTier(),EldritchFaction.ID.toString().equals(allied),"<none>".equals(allied),
            p.getTierProgress(context.getLevel())>=1.0f,bastion,
            bastion&&RiftArena.readyAt(context.getLevel(),at),RiftAudience.available(caster));
    }
    @Override public Component canRitualStart(IRitualContext context){
        var d=decision(context);return d.error()==null?null:Component.m_237113_(d.error());
    }
    @Override protected boolean applyRitualEffect(IRitualContext context){
        var d=decision(context);
        if(d.outcome()==RitualDecision.Outcome.REJECT)return false;
        Player caster=context.getCaster();
        if(d.outcome()==RitualDecision.Outcome.BOSS){
            spawnAt(caster,context.getLevel(),location(context));return running();
        }
        IPlayerProgression p=caster.getCapability(PlayerProgressionProvider.PROGRESSION).orElse(null);
        if(p==null)return false;
        if(d.outcome()==RitualDecision.Outcome.JOIN){
            if(!MnaIntegration.join(caster))return false;
            WarpService.addWarp(caster,WarpService.WarpKind.PERMANENT,1,"Ritual of Gate and Key: initiation");
            message(caster,"The Gate knows your shape. Its gaze has found you; the old circles will turn their backs.");
        }
        if(d.targetTier()>p.getTier()){
            p.setTier(d.targetTier(),caster);
            message(caster,"The Eye acknowledges your understanding. Your studies have reached tier "+d.targetTier()+".");
        }else if(d.outcome()==RitualDecision.Outcome.AUDIENCE){
            message(caster,p.getTier()>=5?"The Eye recognizes one who has passed the final threshold.":
                "The Eye recognizes you, but your studies are not yet complete. Consult the Oculus before seeking further knowledge.");
        }
        // Cosmetic failures must not roll back a committed faction/tier change.
        try{RiftAudience.show(caster);}catch(RuntimeException ex){RuntimeLog.error("Audience unavailable after ritual completion",ex);}
        return true;
    }
    @Override protected int getApplicationTicks(IRitualContext context){return 100;}
}
