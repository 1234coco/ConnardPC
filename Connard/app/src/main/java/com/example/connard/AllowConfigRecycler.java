package com.example.connard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


public class AllowConfigRecycler extends RecyclerView.Adapter<AllowConfigRecycler.ViewHolder> {
    List<GameAllowInfo> gameList;

    public AllowConfigRecycler(List<GameAllowInfo> gameList) {
        this.gameList = gameList;
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtGameName, statusGame;

        public ViewHolder(View itemView) {
            super(itemView);
            txtGameName = itemView.findViewById(R.id.txtGameAllow);
            statusGame = itemView.findViewById(R.id.statusGameAllow);
        }
    }
    @NonNull
    @Override
    public AllowConfigRecycler.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.allow_item_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AllowConfigRecycler.ViewHolder holder, int position) {
        GameAllowInfo game = gameList.get(position);
        holder.txtGameName.setText(game.getName());
        holder.statusGame.setText("Trạng thái: " + game.getStatus());
    }
    public void updateGameList(List<GameAllowInfo> newGameList) {
        this.gameList = newGameList;
        notifyDataSetChanged(); // Thông báo cho adapter rằng dữ liệu đã thay đổi
    }
    @Override
    public int getItemCount() {
        return gameList.size();
    }
}