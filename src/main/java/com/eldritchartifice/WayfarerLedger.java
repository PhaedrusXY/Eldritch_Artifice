package com.eldritchartifice;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Authoritative pair identities; packed tokens cannot deploy a second copy of a placed end. */
final class WayfarerLedger {
 record Place(String dimension,int x,int y,int z,String facing) {
  String key(){return dimension+":"+x+":"+y+":"+z;}
 }
 record End(String token,Place place) {}
 record Pair(End a,End b,String owner) {
  End end(int side){if(side<0||side>1)throw new IllegalArgumentException("side");return side==0?a:b;}
  Pair with(int side,End e){return side==0?new Pair(e,b,owner):new Pair(a,e,owner);}
 }
 private final Map<String,Pair> pairs=new LinkedHashMap<>();
 private final Path file;
 WayfarerLedger(Path file)throws IOException {this.file=file;load();}
 Map<String,Pair> entries(){return Collections.unmodifiableMap(pairs);}
 Pair get(String id){return pairs.get(id);}
 String create()throws IOException {return create(null);}
 String create(String owner)throws IOException {
  if(owner!=null&&owned(owner)!=null)throw new IllegalStateException("Player already has a bound pair");
  String id=UUID.randomUUID().toString();commit(id,new Pair(new End(token(),null),new End(token(),null),owner));return id;
 }
 String owned(String owner){
  for(var e:pairs.entrySet())if(owner.equals(e.getValue().owner()))return e.getKey();
  return null;
 }
 boolean claim(String id,String owner)throws IOException {
  Pair p=pairs.get(id);if(p==null)return false;
  if(owner.equals(p.owner()))return true;
  if(p.owner()!=null||owned(owner)!=null)return false;
  commit(id,new Pair(p.a(),p.b(),owner));return true;
 }
 Pair reissuePacked(String id,String owner)throws IOException {
  Pair p=pairs.get(id);
  if(p==null||!owner.equals(p.owner()))throw new IllegalStateException("Pair belongs to someone else");
  if(p.a().place()!=null&&p.b().place()!=null)throw new IllegalStateException("Both doors are standing");
  Pair issued=new Pair(p.a().place()==null?new End(token(),null):p.a(),p.b().place()==null?new End(token(),null):p.b(),owner);
  commit(id,issued);return issued;
 }
 boolean valid(String id,int side,String token){Pair p=pairs.get(id);return p!=null&&side>=0&&side<2&&p.end(side).token.equals(token);}
 void place(String id,int side,String token,Place place)throws IOException {
  if(!valid(id,side,token)||pairs.get(id).end(side).place!=null)throw new IllegalStateException("Door is already unfolded or token is obsolete");
  for(Pair p:pairs.values())for(End e:List.of(p.a,p.b))if(e.place!=null&&e.place.key().equals(place.key()))throw new IllegalStateException("Occupied threshold");
  commit(id,pairs.get(id).with(side,new End(token,place)));
 }
 void rollbackPlacement(String id,int side)throws IOException {Pair p=pairs.get(id);commit(id,p.with(side,new End(p.end(side).token,null)));}
 End pack(String id,int side)throws IOException {
  Pair p=pairs.get(id);if(p==null||p.end(side).place==null)throw new IllegalStateException("Already packed");
  End end=new End(token(),null);commit(id,p.with(side,end));return end;
 }
 private static String token(){return UUID.randomUUID().toString();}
 private void commit(String id,Pair pair)throws IOException {
  Pair old=pairs.put(id,pair);
  try{save();}catch(IOException ex){if(old==null)pairs.remove(id);else pairs.put(id,old);throw ex;}
 }
 private void save()throws IOException {
  Properties data=new Properties();data.setProperty("format","1");
  for(var entry:pairs.entrySet())for(int side=0;side<2;side++){
   String k=entry.getKey()+"."+side+".";End end=entry.getValue().end(side);data.setProperty(k+"token",end.token);
   if(side==0&&entry.getValue().owner()!=null)data.setProperty(entry.getKey()+".owner",entry.getValue().owner());
   if(end.place!=null){Place p=end.place;data.setProperty(k+"dimension",p.dimension);data.setProperty(k+"x",""+p.x);data.setProperty(k+"y",""+p.y);data.setProperty(k+"z",""+p.z);data.setProperty(k+"facing",p.facing);}
  }
  Files.createDirectories(file.getParent());Path tmp=file.resolveSibling(file.getFileName()+".tmp");
  try(FileOutputStream out=new FileOutputStream(tmp.toFile())){data.store(out,"Eldritch Artifice portable door pairs");out.getChannel().force(true);}
  try{Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException ex){Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}
 }
 private void load()throws IOException {
  if(!Files.exists(file))return;
  Properties data=new Properties();try(InputStream in=Files.newInputStream(file)){data.load(in);}
  if(!"1".equals(data.getProperty("format")))throw new IOException("Unsupported wayfarer ledger");
  try {
   Set<String> ids=new HashSet<>();for(String key:data.stringPropertyNames())if(key.endsWith(".token"))ids.add(key.substring(0,key.length()-8));
   for(String id:ids){UUID.fromString(id);End[] ends=new End[2];for(int side=0;side<2;side++){
    String k=id+"."+side+".";String token=data.getProperty(k+"token");UUID.fromString(token);Place p=null;
    if(data.containsKey(k+"dimension")){String facing=data.getProperty(k+"facing");if(!Set.of("north","south","east","west").contains(facing))throw new IllegalArgumentException("facing");
     p=new Place(data.getProperty(k+"dimension"),Integer.parseInt(data.getProperty(k+"x")),Integer.parseInt(data.getProperty(k+"y")),Integer.parseInt(data.getProperty(k+"z")),facing);}
    ends[side]=new End(token,p);
   }String owner=data.getProperty(id+".owner");if(owner!=null)UUID.fromString(owner);
    pairs.put(id,new Pair(ends[0],ends[1],owner));}
  }catch(RuntimeException ex){throw new IOException("Invalid wayfarer ledger; original file retained",ex);}
 }
}
