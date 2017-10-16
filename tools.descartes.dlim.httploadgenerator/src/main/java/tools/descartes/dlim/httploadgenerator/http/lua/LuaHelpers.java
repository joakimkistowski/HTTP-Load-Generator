/**
 * Copyright 2017 Joakim von Kistowski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tools.descartes.dlim.httploadgenerator.http.lua;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

/**
 * Helper class for LUA scripts. This class helps to create LUA tables of String data types and to transform
 * existing LUA tables of the data types string, integer, double and boolean to array lists.
 * The LuaHelpers provides a function for executing a script file.
 * 
 * 
 * @author Joakim von Kistowski, Maximilian Deffner
 *
 */
public final class LuaHelpers {

	private LuaHelpers() {
		
	}
	
	/**
	 * Execute a LUA script file defined by the path of the script.
	 * 
	 * @param scriptPath
	 * 				Path of the LUA script file.
	 * @return
	 * 		The LUA globals.
	 */
	public static LuaValue execute(String scriptPath) {

		Globals globals = JsePlatform.standardGlobals();
		globals.get("dofile").call(LuaValue.valueOf(scriptPath));
		return globals;
	}

	/**
	 * Transforming a table of LUA values to a array list of strings.
	 * @param table The table to map.
	 * @return The String list.
	 */
	public static ArrayList<String> getStringListFromTable(LuaValue table) {
		return getGenericListFromTable(table, new LuaValueMapper<String>() {
			@Override
			public String map(LuaValue v) {
				return v.tojstring();
			}
		});
	}

	/**
	 * Transforming a table of LUA values to a array list of doubles.
	 * @param table The table to map.
	 * @return The Double list.
	 */
	public static ArrayList<Double> getDoubleListFromTable(LuaValue table) {
		return getGenericListFromTable(table, new LuaValueMapper<Double>() {
			@Override
			public Double map(LuaValue v) {
				return v.todouble();
			}
		});
	}

	/**
	 * Transforming a table of LUA values to a array list of integers.
	 * @param table The table to map.
	 * @return The Integer list.
	 */
	public static ArrayList<Integer> getIntegerListFromTable(LuaValue table) {
		return getGenericListFromTable(table, new LuaValueMapper<Integer>() {
			@Override
			public Integer map(LuaValue v) {
				return v.toint();
			}
		});
	}

	/**
	 * Transforming a table of LUA values to a array list of booleans.
	 * @param table The table to map.
	 * @return The Boolean list.
	 */
	public static ArrayList<Boolean> getBooleanListFromTable(LuaValue table) {
		return getGenericListFromTable(table, new LuaValueMapper<Boolean>() {
			@Override
			public Boolean map(LuaValue v) {
				return v.toboolean();
			}
		});
	}

	/**
	 * Transforming a table of LUA values to a array list of generic data types.
	 * @param table The table.
	 * @param mapper The mapper.
	 * @param <T> The generic type.
	 * @return The mapped arraylist.
	 */
	private static <T> ArrayList<T> getGenericListFromTable(LuaValue table, LuaValueMapper<T> mapper) {
		ArrayList<T> generics = new ArrayList<T>();
		if (!table.isnil() && table.istable()) {
			int i = 1;
			while (true) {
				LuaValue v = table.get(i);
				if (v.isnil()) {
					break;
				}
				generics.add(mapper.map(v));
				i++;
			}
		}
		return generics;
	}

	/**
	 * Transforming a table of LUA values to a array list of LUA Values data type.
	 * @param table A LUA table.
	 * @return The ArrayList map.
	 */
	public static ArrayList<LuaValue> getListFromTable(LuaValue table) {
		return getGenericListFromTable(table, new LuaValueMapper<LuaValue>() {
			@Override
			public LuaValue map(LuaValue v) {
				return v;
			}
		});
	}

	/**
	 * Getting the size of a LUA Value table.
	 * @param table a LUA table.
	 * @return Size of the table.
	 */
	public static int tableSize(LuaValue table) {
		int i = 1;
		if (!table.isnil() && table.istable()) {
			while (true) {
				LuaValue v = table.get(i);
				if (v.isnil()) {
					break;
				}
				i++;
			}
		}
		return i - 1;
	}

	/**
	 * Interface for a generic type of the LUA value mapper.
	 * @param <T> Type of mapped value.
	 */
	private static interface LuaValueMapper<T> {

		public T map(LuaValue v);

	}

	/**
	 * Helper method for reading content of files like a LUA script file.
	 * @param path Path of the script file.
	 * @return The file content.
	 */
	public static String readTextFile(String path) {
		File scriptfile = new File(path);
		String script = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader(scriptfile));
			String line;
			while ((line = br.readLine()) != null) {
				script += line + "\n";
			}
			br.close();
		} catch (FileNotFoundException e) {
			System.out.println("Text file not found at " + scriptfile.getAbsolutePath());
		} catch (IOException e) {
			System.out.println("Read error for text file at " + scriptfile.getAbsolutePath());
		}
		return script;
	}

	/**
	 * Transforming a collection of strings to a LUA value table.
	 * 
	 * @param collection
	 * 				Collection of strings.
	 * @return
	 * 				LUA value table.
	 */
	public static LuaValue toLuaTable(Collection<String> collection) {
		LuaValue table = LuaValue.tableOf();
		int i = 1;
		for (String s : collection) {
			table.set(i, LuaValue.valueOf(s));
			i++;
		}
		return table;
	}
}
