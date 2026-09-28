package com.eldritchartifice;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import static com.eldritchartifice.ShoggothService.*;

/** One designated arena per world. The on-disk journal precedes every terrain mutation. */
final class RiftArena {
    static Object level;
    static double[] center;
    static final LinkedHashMap<String,String> originals = new LinkedHashMap<>();
    static final List<String> available = new ArrayList<>();
    static Properties data = new Properties();
    static Path file;
    static boolean pending;
    static final Set<String> CONSUMABLE = Set.of("minecraft:stone","minecraft:cobblestone",
            "minecraft:mossy_cobblestone","minecraft:stone_bricks","minecraft:cracked_stone_bricks",
            "minecraft:mossy_stone_bricks","minecraft:deepslate","minecraft:cobbled_deepslate",
            "minecraft:deepslate_bricks","minecraft:polished_deepslate","minecraft:andesite",
            "minecraft:polished_andesite","minecraft:diorite","minecraft:granite",
            "minecraft:dirt","minecraft:grass_block","minecraft:netherrack","minecraft:blackstone",
            "minecraft:polished_blackstone_bricks","minecraft:obsidian");

    static void started(Object event) {
        Object server=call(event,"getServer");
        originals.clear();available.clear();data=new Properties();center=null;level=null;pending=false;
        try {
            Class<?> resource=Class.forName("net.minecraft.world.level.storage.LevelResource");
            Object root;
            try { root=resource.getField("ROOT").get(null); }
            catch(NoSuchFieldException ex) {root=resource.getField("f_78182_").get(null);}
            file=((Path)call(server,"getWorldPath|m_129843_",root)).resolve("data/eldritchartifice-rift.properties");
            if(!Files.exists(file))return;
            try(InputStream in=Files.newInputStream(file)){data.load(in);}
            String dimension=data.getProperty("dimension");
            for(Object candidate:(Iterable<?>)call(server,"getAllLevels|m_129785_")) {
                Object key=call(candidate,"dimension|m_46472_");
                if(RuntimeMinecraft.resourceKeyToString(key).equals(dimension)){level=candidate;break;}
            }
            if(level==null)throw new IllegalStateException("Rift dimension is unavailable: "+dimension);
            center=new double[]{Double.parseDouble(data.getProperty("x")),Double.parseDouble(data.getProperty("y")),Double.parseDouble(data.getProperty("z"))};
            pending=Boolean.parseBoolean(data.getProperty("pending","false"));
            for(String key:data.stringPropertyNames())if(key.startsWith("block."))originals.put(key.substring(6),data.getProperty(key));
            if(pending)restore();
            ShoggothService.bar("bossbar remove eldritchartifice:rift");
        }catch(Exception ex){pending=true;throw new IllegalStateException("Rift recovery failed; journal retained",ex);}
    }

    static void mark(Object player) {
        if(pending || ShoggothService.running()) {message(player,"Clear/recover the current encounter before moving its anchor.");return;}
        Object world=RuntimeMinecraft.level(player);double[] p=pos(player);
        int px=(int)Math.floor(p[0]),py=(int)Math.floor(p[1]),pz=(int)Math.floor(p[2]);
        double best=Double.MAX_VALUE;int[] door=null;
        for(int x=px-6;x<=px+6;x++)for(int y=py-3;y<=py+3;y++)for(int z=pz-6;z<=pz+6;z++){
            String state=spec(state(world,x,y,z));
            if(state.startsWith("minecraft:iron_door[")&&state.contains("half=lower")){
                double d=distance(p,new double[]{x+.5,y,z+.5});
                if(d<best){best=d;door=new int[]{x,y,z};}
            }
        }
        if(door==null){message(player,"Place an iron door in your structure and stand within six blocks, then run /eldritch riftanchor.");return;}
        level=world;center=new double[]{door[0]+.5,door[1],door[2]+.5};data=new Properties();
        data.setProperty("dimension",RuntimeMinecraft.currentDimension(player));
        data.setProperty("x",""+center[0]);data.setProperty("y",""+center[1]);data.setProperty("z",""+center[2]);
        save();message(player,"Rift doorway anchored. Perform the Ritual of Gate and Key nearby to open it.");
    }

    static boolean ready(Object caster) {
        return readyAt(RuntimeMinecraft.level(caster),pos(caster));
    }
    static boolean readyAt(Object world,double[] position) {
        return center!=null&&!pending&&!ShoggothService.running()&&world==level
            &&distance(position,center)<=16&&spec(state(level,x(),y(),z())).startsWith("minecraft:iron_door[");
    }

    static void begin() {
        if(pending)throw new IllegalStateException("Unrestored rift journal");
        originals.clear();available.clear();
        originals.put(key(x(),y(),z()),spec(state(level,x(),y(),z())));
        originals.put(key(x(),y()+1,z()),spec(state(level,x(),y()+1,z())));
        // Deterministic finite set. Do not consume containers, machines, fluids or gravity blocks.
        List<String> candidates=new ArrayList<>();
        for(int x=x()-10;x<=x()+10;x++)for(int y=y()-2;y<=y()+6;y++)for(int z=z()-10;z<=z()+10;z++){
            if(distance(new double[]{x+.5,y,z+.5},center)>10)continue;
            Object block=state(level,x,y,z);String s=spec(block);String id=s.split("\\[",2)[0];
            if(CONSUMABLE.contains(id)&&call(level,"getBlockEntity|m_7702_",blockPos(x,y,z))==null)candidates.add(key(x,y,z));
        }
        Collections.shuffle(candidates,new Random(0xE1D17C));
        for(String k:candidates.subList(0,Math.min(240,candidates.size()))){int[] p=parse(k);originals.put(k,spec(state(level,p[0],p[1],p[2])));available.add(k);}
        for(var e:originals.entrySet())data.setProperty("block."+e.getKey(),e.getValue());
        pending=true;data.setProperty("pending","true");save(); // durable BEFORE any block is changed
        set(key(x(),y()+1,z()),"minecraft:air");set(key(x(),y(),z()),"minecraft:air");
    }

    static Object[] consume() {
        if(available.isEmpty())return null;
        String k=available.remove(available.size()-1);int[] p=parse(k);Object before=state(level,p[0],p[1],p[2]);
        if(!spec(before).equals(originals.get(k)))return null;
        set(k,"minecraft:air");
        return new Object[]{new double[]{p[0]+.5,p[1]+.5,p[2]+.5},before};
    }

    static void restore() {
        if(!pending||level==null)return;
        // Load only journaled chunks on recovery; no permanent chunk tickets.
        for(var entry:originals.entrySet()) {
            int[] p=parse(entry.getKey());
            call(level,"getChunk|m_6325_",p[0]>>4,p[2]>>4);
            if(call(level,"getBlockEntity|m_7702_",blockPos(p[0],p[1],p[2]))!=null)
                throw new IllegalStateException("Recovery blocked by a container/machine at "+entry.getKey()+"; journal retained");
            set(entry.getKey(),entry.getValue());
        }
        // Flush restored chunk data BEFORE clearing the recovery journal. Otherwise a crash
        // between journal deletion and the next autosave could leave permanent holes.
        call(call(level,"getChunkSource|m_7726_"),"save|m_8419_",true);
        for(String k:new ArrayList<>(data.stringPropertyNames()))if(k.startsWith("block."))data.remove(k);
        data.setProperty("pending","false");save();pending=false;originals.clear();available.clear();
        RuntimeLog.info("Restored rift arena and doorway.");
    }

    static boolean inside(Object world,Object bp) {
        if(!pending||world!=level||center==null)return false;
        double[] p={((Number)call(bp,"getX|m_123341_")).doubleValue()+.5,
            ((Number)call(bp,"getY|m_123342_")).doubleValue(),((Number)call(bp,"getZ|m_123343_")).doubleValue()+.5};
        return distance(p,center)<=12;
    }
    static void protect(Object event){if(inside(call(event,"getLevel"),call(event,"getPos")))call(event,"setCanceled",true);}
    static void explosion(Object event){if(level==call(event,"getLevel")&&pending)((List<?>)call(event,"getAffectedBlocks")).removeIf(bp->inside(level,bp));}
    static int x(){return (int)Math.floor(center[0]);}static int y(){return (int)Math.floor(center[1]);}static int z(){return (int)Math.floor(center[2]);}
    static String key(int x,int y,int z){return x+","+y+","+z;}
    static int[] parse(String key){return Arrays.stream(key.split(",")).mapToInt(Integer::parseInt).toArray();}
    static Object blockPos(int x,int y,int z){try{return Class.forName("net.minecraft.core.BlockPos").getConstructor(int.class,int.class,int.class).newInstance(x,y,z);}catch(Exception ex){throw new IllegalStateException(ex);}}
    static Object state(Object level,int x,int y,int z){return call(level,"getBlockState|m_8055_",blockPos(x,y,z));}
    static String spec(Object state){String s=state.toString();int end=s.indexOf('}');if(!s.startsWith("Block{")||end<6)throw new IllegalStateException("Unexpected block state "+s);return s.substring(6,end)+s.substring(end+1);}
    static void set(String key,String spec){
        try{
            Class<?> registries=Class.forName("net.minecraft.core.registries.BuiltInRegistries");Object blocks;
            try{blocks=registries.getField("BLOCK").get(null);}catch(NoSuchFieldException ex){blocks=registries.getField("f_256975_").get(null);}
            Object lookup=call(blocks,"asLookup|m_255303_");
            Object parsed=staticCall("net.minecraft.commands.arguments.blocks.BlockStateParser","parseForBlock|m_245437_",lookup,spec,false);
            Object state=call(parsed,"blockState|f_234748_");int[] p=parse(key);Object bp=blockPos(p[0],p[1],p[2]);
            call(level,"getChunk|m_6325_",p[0]>>4,p[2]>>4);
            call(level,"setBlock|m_7731_",bp,state,18); // clients + no neighbor shape updates: no cascading drops
            if(!spec(state(level,p[0],p[1],p[2])).equals(spec))throw new IllegalStateException("Block restore/write rejected at "+key);
        }catch(Exception ex){throw new IllegalStateException("Rift block write failed at "+key,ex);}
    }
    static void save(){
        if(file==null)throw new IllegalStateException("Rift storage has not initialized");
        try{Files.createDirectories(file.getParent());Path tmp=file.resolveSibling(file.getFileName()+".tmp");
            try(OutputStream out=Files.newOutputStream(tmp)){data.store(out,"Eldritch rift recovery journal; do not edit during encounter");}
            try(FileChannel ch=FileChannel.open(tmp,StandardOpenOption.WRITE)){ch.force(true);}
            Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        }catch(IOException ex){throw new IllegalStateException("Could not persist rift arena",ex);}
    }
    static int command(Object world,String text){
        Object server=call(world,"getServer|m_7654_");Object source=call(server,"createCommandSourceStack|m_129893_");
        source=call(source,"withLevel|m_81327_",world);source=call(source,"withSuppressedOutput|m_81324_");source=call(source,"withPermission|m_81325_",4);
        Object dispatcher=call(call(server,"getCommands|m_129892_"),"getDispatcher|m_82094_");
        return ((Number)call(dispatcher,"execute",text,source)).intValue();
    }
}
