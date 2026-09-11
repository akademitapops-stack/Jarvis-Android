package com.hermes.jarvis.adapter;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.hermes.jarvis.R;
import com.hermes.jarvis.model.Message;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {

    private final List<Message> items = new ArrayList<>();
    private final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Message m = items.get(pos);
        h.tvMsg.setText(m.text);
        h.tvTime.setText(fmt.format(new Date(m.time)));
        h.tvMsg.setTypeface(Typeface.MONOSPACE);

        LinearLayout.LayoutParams p = (LinearLayout.LayoutParams)
                h.container.getLayoutParams();

        switch (m.type) {
            case Message.USER:
                h.container.setBackground(ContextCompat.getDrawable(
                        h.itemView.getContext(), R.drawable.bg_msg_user));
                h.tvMsg.setTextColor(Color.WHITE);
                h.tvTime.setTextColor(0xB0FFFFFF);
                p.gravity = Gravity.END;
                p.setMargins(80, 4, 8, 4);
                break;
            case Message.TERM:
                h.container.setBackground(ContextCompat.getDrawable(
                        h.itemView.getContext(), R.drawable.bg_msg_term));
                h.tvMsg.setTextColor(Color.parseColor("#3FB950"));
                h.tvMsg.setTextSize(11f);
                h.tvTime.setTextColor(Color.parseColor("#8B949E"));
                p.gravity = Gravity.FILL_HORIZONTAL;
                p.setMargins(8, 4, 8, 4);
                break;
            case Message.ERROR:
                h.container.setBackground(ContextCompat.getDrawable(
                        h.itemView.getContext(), R.drawable.bg_msg_err));
                h.tvMsg.setTextColor(Color.parseColor("#F85149"));
                h.tvTime.setTextColor(Color.parseColor("#8B949E"));
                p.gravity = Gravity.FILL_HORIZONTAL;
                p.setMargins(8, 4, 8, 4);
                break;
            case Message.INFO:
                h.container.setBackground(ContextCompat.getDrawable(
                        h.itemView.getContext(), R.drawable.bg_msg_info));
                h.tvMsg.setTextColor(Color.parseColor("#42A5F5"));
                h.tvTime.setTextColor(Color.parseColor("#8B949E"));
                p.gravity = Gravity.FILL_HORIZONTAL;
                p.setMargins(8, 4, 8, 4);
                break;
            case Message.OK:
                h.container.setBackground(ContextCompat.getDrawable(
                        h.itemView.getContext(), R.drawable.bg_msg_ok));
                h.tvMsg.setTextColor(Color.parseColor("#4CAF50"));
                h.tvTime.setTextColor(Color.parseColor("#8B949E"));
                p.gravity = Gravity.FILL_HORIZONTAL;
                p.setMargins(8, 4, 8, 4);
                break;
            default:
                h.container.setBackground(ContextCompat.getDrawable(
                        h.itemView.getContext(), R.drawable.bg_msg_bot));
                h.tvMsg.setTextColor(Color.parseColor("#E6EDF3"));
                h.tvTime.setTextColor(Color.parseColor("#8B949E"));
                p.gravity = Gravity.START;
                p.setMargins(8, 4, 80, 4);
                break;
        }
        h.container.setLayoutParams(p);
    }

    @Override public int getItemCount() { return items.size(); }

    public void add(Message m) {
        items.add(m);
        notifyItemInserted(items.size() - 1);
    }
    public void clear() { items.clear(); notifyDataSetChanged(); }

    static class VH extends RecyclerView.ViewHolder {
        LinearLayout container;
        TextView tvMsg, tvTime;
        VH(@NonNull View v) {
            super(v);
            container = v.findViewById(R.id.messageContainer);
            tvMsg = v.findViewById(R.id.tvMessage);
            tvTime = v.findViewById(R.id.tvTime);
        }
    }
}
