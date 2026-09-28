package com.eldritchartifice;

/** Small, testable destination geometry; native collision checks remain authoritative. */
final class WayfarerExit {
 private WayfarerExit() {}
 static double center(int coordinate,int direction,int side) {
  return coordinate+0.5+direction*side;
 }
 static boolean hazard(String state) {
  String id=state.split("\\[",2)[0];
  String path=id.substring(id.indexOf(':')+1);
  return path.equals("lava")||path.equals("fire")||path.equals("soul_fire")
   ||path.equals("cactus")||path.equals("magma_block")||path.equals("powder_snow")
   ||((path.equals("campfire")||path.equals("soul_campfire"))&&!state.contains("lit=false"));
 }
}
