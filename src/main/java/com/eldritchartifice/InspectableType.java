package com.eldritchartifice;
import java.util.concurrent.ConcurrentHashMap;
/** Use inherited public APIs when a mod subclass has an unavailable signature type. */
final class InspectableType {
 private static final ConcurrentHashMap<Class<?>,Class<?>> CACHE=new ConcurrentHashMap<>();
 private static final ConcurrentHashMap<Class<?>,java.lang.reflect.Method[]> METHODS=new ConcurrentHashMap<>();
 static java.lang.reflect.Method[] methods(Class<?> type){return METHODS.computeIfAbsent(type,t->of(t).getMethods());}
 static Class<?> of(Class<?> type){return CACHE.computeIfAbsent(type,InspectableType::resolve);}
 private static Class<?> resolve(Class<?> type){
  NoClassDefFoundError failure=null;
  for(Class<?> candidate=type;candidate!=null;candidate=candidate.getSuperclass()){
   try{candidate.getMethods();
    if(candidate!=type)RuntimeLog.warn("Using inherited API for "+type.getName()+" because a method signature references an unavailable class: "+failure.getMessage());
    return candidate;
   }catch(NoClassDefFoundError missing){failure=missing;}
  }
  throw failure;
 }
}
