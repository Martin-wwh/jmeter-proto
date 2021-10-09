package com.mapi.utils;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import org.json.JSONObject;

public class MappingField {

    private String packageDir = "com.mapi";

    public  MappingField(){

    }

    public int getCount(String str,String key){
        if(str == null || key == null || "".equals(str.trim()) || "".equals(key.trim()))
        {
            return 0;
        }
        int count = 0;
        int index = 0;
        while((index=str.indexOf(key,index))!=-1){
            index = index+key.length();
            count++;
        }
        return count;
    }

    public void writeHashMapTo(HashMap map,String Pkey, String path){
        JSONObject jsonObject = new JSONObject(map);
        URL url = this.getClass().getResource("/");
        String pathUrl = url.getPath();
        String projectPath = System.getProperty("user.dir");
        String projectName = projectPath.split("\\\\")[projectPath.split("\\\\").length-1];
        pathUrl = pathUrl.substring(0, pathUrl.indexOf(projectName));
        pathUrl = new File(pathUrl).getAbsolutePath();
        String[] wholePathList = {pathUrl, projectName, "src", "main", "resources", "gameproto",Pkey,"settings"};
        String wholePath = WereTransformUtils.pathJoin(wholePathList);
        File fileDir = new File(wholePath);
        if (!fileDir.exists()){
            fileDir.mkdirs();
        }
        File file = new File(fileDir + File.separator + path);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        try {
            FileWriter fileWriter = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            jsonObject.write(bufferedWriter);
            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void writeArrayTo(Object[] maps,String Pkey, String path){
        JSONArray jsonArray = (JSONArray) JSONArray.toJSON(maps);
        String jsonStr = jsonArray.toJSONString();
        URL url = this.getClass().getResource("/");
        String pathUrl = url.getPath();
        String projectPath = System.getProperty("user.dir");
        String projectName = projectPath.split("\\\\")[projectPath.split("\\\\").length-1];
        pathUrl = pathUrl.substring(0, pathUrl.indexOf(projectName));
        pathUrl = new File(pathUrl).getAbsolutePath();
        String[] wholePathList = {pathUrl, projectName, "src", "main", "resources", "gameproto",Pkey,"settings"};
        String wholePath = WereTransformUtils.pathJoin(wholePathList);
        File fileDir = new File(wholePath);
        if (!fileDir.exists()){
            fileDir.mkdirs();
        }
        File file = new File(fileDir + File.separator + path);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        try {

            PrintWriter printWriter = new PrintWriter(file);
            printWriter.print(jsonStr);
//            printWriter.write(jsonStr);
            printWriter.flush();
            printWriter.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void getAll(String Pkey){
        File f = new File(this.getClass().getResource("/").getPath());
        String path = f.getAbsolutePath();
        String[] currents = {path, packageDir.replace(".", File.separator), Pkey};
        String curr_pack = WereTransformUtils.pathJoin(currents);
        String wholePkey = packageDir + "." + Pkey;
        ArrayList<HashMap<String, String>> mainClasses = new ArrayList<>();
        searchAllClasses(curr_pack, wholePkey, mainClasses);
        ArrayList<HashMap<String, String>> data = new ArrayList<>();
        for (HashMap<String, String> hashMap:
                mainClasses) {
            String packagePath = hashMap.get("packagePath");
            try {
                Class mainClass = Class.forName(packagePath);
                Class[] innerClasses = mainClass.getDeclaredClasses();
                for (Class innerClass:
                        innerClasses) {
                    HashMap<String,String> result = new HashMap<>();
                    String messageClass = innerClass.getName();
                    String mname = messageClass.split("\\$")[messageClass.split("\\$").length-1];
                    if (messageClass.contains("Builder")) continue;
                    String packpath = mainClass.getName();
                    String[] packlist = packpath.split("\\.");
                    String packages = packlist[packlist.length-2];
                    String packname = packlist[packlist.length-1];
                    result.put("mname", mname);
                    result.put("packpath", packpath);
                    result.put("packname", packname);
                    result.put("package", packages);
                    data.add(result);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        HashSet<String> packPathSet = new HashSet<>();
        for (HashMap<String,String> hashMap:
                data) {
            packPathSet.add(hashMap.get("packpath"));
        }
        String[] packPathList = new String[packPathSet.size()];
        packPathSet.toArray(packPathList);
        HashMap<String, ArrayList<HashMap>> packpathdict = new HashMap<>();
        for (String packPath:
                packPathList) {
            packpathdict.put(packPath, new ArrayList());
        }
        ArrayList<HashMap> res = new ArrayList<>();
        for(HashMap<String,String> map:data){
            HashMap<String,String> child = new HashMap<>();
            child.put("mname", map.get("mname"));
            child.put("packname", map.get("packname"));
            child.put("package", map.get("package"));
            packpathdict.get(map.get("packpath")).add(child);
        }
        int index = 1;
        for(String k:packpathdict.keySet()){
            HashMap<String, Object> pardata = new HashMap<>();
            pardata.put("id", index);
            pardata.put("packpath", k);
            pardata.put("packname", packpathdict.get(k).get(0).get("packname"));
            pardata.put("package", packpathdict.get(k).get(0).get("package"));
            pardata.put("children", new ArrayList<HashMap>());
            int cid = 1;
            for(HashMap<String, String> i:packpathdict.get(k)){
                HashMap<String, Object> cdata = new HashMap<>();
                cdata.put("id", cid);
                cdata.put("mname", i.get("mname"));
                ArrayList<HashMap> children = (ArrayList) pardata.get("children");
                children.add(cdata);
                cid ++;
            }
            res.add(pardata);
            index ++;
        }
        HashMap apidata = new HashMap(), rapidata = new HashMap();
        HashMap<Integer,String> newData = new HashMap();
        HashSet<String> namelist = new HashSet();
        index = 0;
        for(HashMap i:res){
            ArrayList<HashMap> children = (ArrayList) i.get("children");
            for(HashMap k:children){
                index ++;
                namelist.add((String) i.get("package"));
                String name = i.get("package") + "." + k.get("mname");
                long pid = new StringHash().getHash(name);
                HashMap<String, Object> apidataItem = new HashMap<>();
                apidataItem.put("mname", k.get("mname"));
                apidataItem.put("packpath", i.get("packpath"));
                apidataItem.put("package", i.get("package"));
                apidataItem.put("packname", i.get("packname"));
                apidataItem.put("pid", pid);
                apidata.put(name, apidataItem);

                HashMap<String, Object> rapidataItem = new HashMap<>();
                rapidataItem.put("packpath", i.get("packpath"));
                rapidataItem.put("package", i.get("package"));
                rapidataItem.put("packname", i.get("packname"));
                rapidataItem.put("mname", k.get("mname"));
                newData.put(index, name);
                rapidata.put(pid, rapidataItem);

            }
        }
        writeHashMapTo(apidata,Pkey,"sendtest.json");
        writeHashMapTo(rapidata,Pkey,"recvtest.json");
        writeHashMapTo(newData, Pkey, "api.json");
        Map<String, ArrayList> newNameList = new HashMap<>();
        for (String i:
                namelist) {
            newNameList.put(i, new ArrayList());
        }
        for (int k:
                newData.keySet()) {
            String v = newData.get(k);
            String label = v.split("\\.")[0];
            ArrayList labelItem = newNameList.get(label);
            HashMap item = new HashMap();
            item.put("id", k);
            item.put("name", v);
            labelItem.add(item);
        }
        index = 0;
        ArrayList<HashMap> newRes = new ArrayList();
        for(String k:newNameList.keySet()){
            index ++;
            HashMap item = new HashMap();
            item.put("id", index);
            item.put("package", k);
            item.put("children", newNameList.get(k));
            newRes.add(item);
        }
        writeArrayTo(newRes.toArray(), Pkey,"res.json");

    }

    public void searchAllClasses(String packDir, String Pkey, ArrayList<HashMap<String, String>> classesPath){

        File packFile = new File(packDir);
        if(packFile.exists()){
            File[] files = packFile.listFiles();

            if(files != null){
                for(File fileChildDir:files){
                    if(fileChildDir.isDirectory()){
                        String thePkey = new String(Pkey);
                        thePkey += "." + fileChildDir.getName();
                        searchAllClasses(fileChildDir.getAbsolutePath(), thePkey, classesPath);
                    }
                    if(fileChildDir.isFile()){
                        if(!fileChildDir.getName().endsWith("class") || fileChildDir.getName().contains("$"))
                            continue;
                        String mainClass = fileChildDir.getName().substring(0, fileChildDir.getName().length()-6);
                        String packagePath = Pkey + "." + mainClass;
                        String[] packList = packagePath.split("\\.");
                        String lastPack = packList[packList.length-2];
                        HashMap<String, String> map = new HashMap<>();
                        map.put("mainClass", mainClass);
                        map.put("packagePath", packagePath);
                        map.put("lastPack", lastPack);
                        classesPath.add(map);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        MappingField mappingField = new MappingField();
        mappingField.getAll("werewolf");
    }
}
