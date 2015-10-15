/**
 *  Copyright (C) Zhang,Yuexiang (xfeep)
 *
 */
package nginx.clojure.java;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import nginx.clojure.MiniConstants;
import nginx.clojure.NginxClojureRT;
import nginx.clojure.util.NginxSharedHashMap;

public class SharedMapTestSet4NginxJavaRingHandler implements NginxJavaRingHandler {
	
	private Map<String, NginxJavaRingHandler> routing = new HashMap<String, NginxJavaRingHandler>();
	
	public static abstract class SharedMapBaseHandler implements NginxJavaRingHandler {
		protected abstract String name();
		protected NginxSharedHashMap map; 
		
		public SharedMapBaseHandler() {
			map = NginxSharedHashMap.build(name());
		}
		
		public Object[] invoke(Map<String, Object> request) throws IOException{
			String qs = (String) request.get(MiniConstants.QUERY_STRING);
			Map<String, String> params = new HashMap<String, String>();
		
			String[] pvs = qs != null ? qs.split("&") : new String[0];
			for (String pv : pvs) {
				String[] pva = pv.split("=");
				params.put(URLDecoder.decode(pva[0], "utf-8"), URLDecoder.decode(pva[1], "utf-8"));
			}
			
			String op = (String) params.get("op");
			NginxClojureRT.getLog().info("qs:"+qs+", op:"+op);
			
			String key = params.get("key");
			String val = params.get("val");
			Object oval;
			String rt = "<No-Value>";
			
			if (op.equals("put")) {
				oval = map.put(key, val);
				NginxClojureRT.getLog().info(rt = "put:" + key + ":" + val + ", old=" + oval + ", size=" + map.size());
			} else if (op.equals("get")) {
				oval = map.get(key);
				NginxClojureRT.getLog().info(rt = "get:" + key + ":" + oval + ", size=" + map.size());
			} else if (op.equals("remove")) {
				oval = map.remove(key);
				NginxClojureRT.getLog().info(rt = "remove:" + key + ":" + oval + ", size=" + map.size());
			}else if (op.equals("puti")) {
				int old = map.putInt(key, Integer.parseInt(val));
				NginxClojureRT.getLog().info(rt = "puti:" + key + ":" + val + ", old=" + old + ", size=" + map.size());
			}else if (op.equals("putl")) {
				long old = map.putLong(key, Long.parseLong(val));
				NginxClojureRT.getLog().info(rt = "putl:" + key + ":" + val + ", old=" + old + ", size=" + map.size());
			}else if (op.equals("atomici")) {
				val = params.get("delta");
				int old = map.atomicAddInt(key, Integer.parseInt(val));
				NginxClojureRT.getLog().info(rt = "atomici:" + key + ":" + val + ", old=" + old + ", size=" + map.size());
			}else if (op.equals("iput")) {
				Object old = map.put(Integer.parseInt(key), val);
				NginxClojureRT.getLog().info(rt = "iput:" + key + ":" + val + ", old=" + old + ", size=" + map.size());
			}else if (op.equals("iget")) {
				Object old = map.get(Integer.parseInt(key));
				NginxClojureRT.getLog().info(rt = "iget:" + key + ":" +  old + ", size=" + map.size());
			}else if (op.equals("iremove")) {
				Object old = map.remove(Integer.parseInt(key));
				NginxClojureRT.getLog().info(rt = "iremove:" + key + ":" +  old + ", size=" + map.size());
			}else if (op.equals("lput")) {
				Object old = map.put(Long.parseLong(key), val);
				NginxClojureRT.getLog().info(rt = "lput:" + key + ":" + val + ", old=" + old + ", size=" + map.size());
			}else if (op.equals("lget")) {
				Object old = map.get(Long.parseLong(key));
				NginxClojureRT.getLog().info(rt = "lget:" + key + ":" +  old + ", size=" + map.size());
			}else if (op.equals("lremove")) {
				Object old = map.remove(Long.parseLong(key));
				NginxClojureRT.getLog().info(rt = "lremove:" + key + ":" +  old + ", size=" + map.size());
			}else if (op.equals("size")) {
				NginxClojureRT.getLog().info(rt = "size:" + map.size());
			}else if (op.equals("clear")) {
				map.clear();
				NginxClojureRT.getLog().info(rt = "size:" + map.size());
			}else if (op.equals("perfi")) {
				long s = System.currentTimeMillis();
				rt = "";
				int c = 1000;
				for (int i = 0; i < c; i++) {
					map.put(i, i);
				}
				rt += "add cost:"+ (System.currentTimeMillis() - s) + "ms\n";
				
				s = System.currentTimeMillis();
				for (int i = 0; i < c; i++) {
					if ((Integer)map.get(i) != i) {
						throw new RuntimeException("wrong return of get");
					}
				}
				rt += "get cost:"+ (System.currentTimeMillis() - s) + "ms\n";
				
				
				s = System.currentTimeMillis();
				for (int i = 0; i < c; i++) {
					if ((Integer)map.remove(i) != i) {
						throw new RuntimeException("wrong return of remove");
					}
				}
				rt += "del cost:"+ (System.currentTimeMillis() - s) + "ms\n";
			}
			
			return new Object[] {200, null, rt};
		}
	}
	
	public static class TinyMapHandler extends SharedMapBaseHandler {
		@Override
		protected String name() {
			return "testTinyMap";
		}
	}
	
	public static class HashMapHandler extends SharedMapBaseHandler {
		@Override
		protected String name() {
			return "testHashMap";
		}
	}

	public SharedMapTestSet4NginxJavaRingHandler() {
		routing.put("/tinyMap", new TinyMapHandler());
		routing.put("/hashMap", new HashMapHandler());
	}

	@Override
	public Object[] invoke(Map<String, Object> request) throws IOException {
		String uri = (String) request.get("uri");
		String path = uri.substring(uri.lastIndexOf('/'));
		return routing.get(path).invoke(request);
	}

}
