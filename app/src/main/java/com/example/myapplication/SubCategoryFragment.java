package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubCategoryFragment extends Fragment {

    private String categoryName;
    private ExpandableListView expandableListView;
    private List<String> groupList;
    private Map<String, List<String>> childMap;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sub_categories, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        categoryName = getArguments() != null ? getArguments().getString("categoryName", "Woman") : "Woman";
        
        TextView titleView = view.findViewById(R.id.txtSubCategoryTitle);
        titleView.setText(categoryName);

        view.findViewById(R.id.btnBackSub).setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        
        view.findViewById(R.id.btnSearchSub).setOnClickListener(v -> 
            Toast.makeText(requireContext(), "Search clicked", Toast.LENGTH_SHORT).show());
        
        view.findViewById(R.id.btnCartSub).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CartActivity.class);
            startActivity(intent);
        });

        expandableListView = view.findViewById(R.id.expandableSubCategories);
        prepareData();
        
        SubCategoryAdapter adapter = new SubCategoryAdapter(requireContext(), groupList, childMap);
        expandableListView.setAdapter(adapter);

        // Optional: Expand first group by default
        // expandableListView.expandGroup(1);
    }

    private void prepareData() {
        groupList = new ArrayList<>();
        childMap = new HashMap<>();

        groupList.add("New");
        groupList.add("Top");
        groupList.add("Dresses");
        groupList.add("Bottom");
        groupList.add("Underwear");
        groupList.add("Jewelry");

        List<String> newList = new ArrayList<>();
        newList.add("Arrived Today");
        newList.add("Best Sellers");
        childMap.put("New", newList);

        List<String> topList = new ArrayList<>();
        topList.add("Basics");
        topList.add("T-Shirts");
        topList.add("Shirts");
        topList.add("Tank tops");
        topList.add("Blouses");
        childMap.put("Top", topList);

        List<String> dressList = new ArrayList<>();
        dressList.add("Mini");
        dressList.add("Midi");
        dressList.add("Maxi");
        childMap.put("Dresses", dressList);

        List<String> bottomList = new ArrayList<>();
        bottomList.add("Pants");
        bottomList.add("Jeans");
        bottomList.add("Skirts");
        bottomList.add("Shorts");
        childMap.put("Bottom", bottomList);

        childMap.put("Underwear", new ArrayList<>());
        childMap.put("Jewelry", new ArrayList<>());
    }

    private static class SubCategoryAdapter extends BaseExpandableListAdapter {
        private final Context context;
        private final List<String> groupList;
        private final Map<String, List<String>> childMap;

        public SubCategoryAdapter(Context context, List<String> groupList, Map<String, List<String>> childMap) {
            this.context = context;
            this.groupList = groupList;
            this.childMap = childMap;
        }

        @Override
        public int getGroupCount() {
            return groupList.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            List<String> children = childMap.get(groupList.get(groupPosition));
            return children != null ? children.size() : 0;
        }

        @Override
        public Object getGroup(int groupPosition) {
            return groupList.get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            List<String> children = childMap.get(groupList.get(groupPosition));
            return children != null ? children.get(childPosition) : null;
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            String title = (String) getGroup(groupPosition);
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_sub_category_group, parent, false);
            }
            TextView txtTitle = convertView.findViewById(R.id.txtGroupTitle);
            ImageView indicator = convertView.findViewById(R.id.imgGroupIndicator);
            txtTitle.setText(title);
            
            indicator.setRotation(isExpanded ? 0 : 180);
            
            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            String title = (String) getChild(groupPosition, childPosition);
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_sub_category_child, parent, false);
            }
            TextView txtTitle = convertView.findViewById(R.id.txtChildTitle);
            txtTitle.setText(title);
            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }
}
