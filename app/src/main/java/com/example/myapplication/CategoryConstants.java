package com.example.myapplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CategoryConstants {
    public static class Group {
        public String name;
        public List<String> subs;

        public Group(String name, String... subs) {
            this.name = name;
            this.subs = Arrays.asList(subs);
        }
    }

    public static Map<String, List<Group>> getCategoryHierarchy() {
        Map<String, List<Group>> hierarchy = new LinkedHashMap<>();

        // Women
        List<Group> womenGroups = new ArrayList<>();
        womenGroups.add(new Group("Top", "Tops", "T-Shirts", "Shirts"));
        womenGroups.add(new Group("Dresses", "Dresses"));
        womenGroups.add(new Group("Bottom", "Pants", "Jeans", "Skirts", "Shorts"));
        womenGroups.add(new Group("Outerwear", "Jackets", "Suits", "Sweaters", "Hoodies"));
        womenGroups.add(new Group("Activewear", "Activewear"));
        womenGroups.add(new Group("Sleepwear", "Sleepwear"));
        womenGroups.add(new Group("Underwear", "Underwear"));
        womenGroups.add(new Group("Beach wear", "Beach wear"));
        hierarchy.put("Women", womenGroups);

        // Men
        List<Group> menGroups = new ArrayList<>();
        menGroups.add(new Group("Top", "Shirts", "T-Shirts"));
        menGroups.add(new Group("Bottom", "Pants", "Jeans", "Shorts"));
        menGroups.add(new Group("Outerwear", "Jackets", "Suits", "Sweaters", "Hoodies"));
        menGroups.add(new Group("Activewear", "Activewear"));
        menGroups.add(new Group("Underwear", "Underwear"));
        menGroups.add(new Group("Sleepwear", "Sleepwear"));
        hierarchy.put("Men", menGroups);

        // Kids
        List<Group> kidsGroups = new ArrayList<>();
        kidsGroups.add(new Group("Top", "T-Shirts", "Shirts"));
        kidsGroups.add(new Group("Dresses", "Dresses"));
        kidsGroups.add(new Group("Bottom", "Pants", "Shorts", "Skirts"));
        kidsGroups.add(new Group("Outerwear", "Jackets", "Sweaters", "Hoodies"));
        kidsGroups.add(new Group("Sleepwear", "Sleepwear"));
        kidsGroups.add(new Group("Underwear", "Underwear"));
        hierarchy.put("Kids", kidsGroups);

        // Home
        List<Group> homeGroups = new ArrayList<>();
        homeGroups.add(new Group("Furniture", "Furniture"));
        homeGroups.add(new Group("Decor", "Decor"));
        homeGroups.add(new Group("Kitchen", "Kitchen"));
        hierarchy.put("Home", homeGroups);

        return hierarchy;
    }

    public static List<String> getFlatSubCategories(String mainCategory) {
        List<Group> groups = getCategoryHierarchy().get(mainCategory);
        List<String> flat = new ArrayList<>();
        if (groups != null) {
            for (Group g : groups) {
                for (String s : g.subs) {
                    if (!flat.contains(s)) flat.add(s);
                }
            }
        }
        return flat;
    }
}
