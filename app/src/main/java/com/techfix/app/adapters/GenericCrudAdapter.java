package com.techfix.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.techfix.app.R;

import java.util.ArrayList;
import java.util.List;

public class GenericCrudAdapter extends RecyclerView.Adapter<GenericCrudAdapter.CrudViewHolder> {

    public static class CrudItem {
        public String id;
        public String title;
        public String subtitle;
        public Object rawObject;

        public CrudItem(String id, String title, String subtitle, Object rawObject) {
            this.id = id;
            this.title = title;
            this.subtitle = subtitle;
            this.rawObject = rawObject;
        }
    }

    public interface CrudActionListener {
        void onEdit(CrudItem item);
        void onDelete(CrudItem item);
    }

    private List<CrudItem> itemsList = new ArrayList<>();
    private CrudActionListener listener;

    public void setItems(List<CrudItem> list) {
        this.itemsList = list;
        notifyDataSetChanged();
    }

    public void setCrudActionListener(CrudActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CrudViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_crud, parent, false);
        return new CrudViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CrudViewHolder holder, int position) {
        CrudItem item = itemsList.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return itemsList.size();
    }

    static class CrudViewHolder extends RecyclerView.ViewHolder {

        private final TextView title, subtitle;
        private final ImageView imgEdit, imgDelete;

        public CrudViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.crudTitle);
            subtitle = itemView.findViewById(R.id.crudSubtitle);
            imgEdit = itemView.findViewById(R.id.imgEdit);
            imgDelete = itemView.findViewById(R.id.imgDelete);
        }

        public void bind(CrudItem item, CrudActionListener listener) {
            title.setText(item.title);
            subtitle.setText(item.subtitle);

            imgEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEdit(item);
            });

            imgDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDelete(item);
            });
        }
    }
}
