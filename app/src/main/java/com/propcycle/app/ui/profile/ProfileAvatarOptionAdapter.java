package com.propcycle.app.ui.profile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.radiobutton.MaterialRadioButton;
import com.propcycle.app.R;
import com.propcycle.app.ui.common.ProfileAvatarRenderer;

import java.util.List;

/** Theme-consistent rows for the built-in profile avatar chooser. */
final class ProfileAvatarOptionAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final List<String> keys;
    private final List<String> labels;
    private final int selectedPosition;

    ProfileAvatarOptionAdapter(
            @NonNull Context context,
            @NonNull List<String> keys,
            @NonNull List<String> labels,
            int selectedPosition) {
        inflater = LayoutInflater.from(context);
        this.keys = keys;
        this.labels = labels;
        this.selectedPosition = selectedPosition;
    }

    @Override
    public int getCount() {
        return Math.min(keys.size(), labels.size());
    }

    @Override
    public String getItem(int position) {
        return keys.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            row = inflater.inflate(R.layout.item_profile_avatar_option, parent, false);
        }
        ImageView icon = row.findViewById(R.id.avatar_option_icon);
        TextView label = row.findViewById(R.id.avatar_option_label);
        MaterialRadioButton selected = row.findViewById(R.id.avatar_option_selected);
        ProfileAvatarRenderer.render(icon, keys.get(position));
        label.setText(labels.get(position));
        selected.setChecked(position == selectedPosition);
        row.setContentDescription(labels.get(position)
                + (position == selectedPosition ? ", selected" : ""));
        return row;
    }
}
