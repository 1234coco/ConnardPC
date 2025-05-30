package com.example.connard;



import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


public class PauseConfigRecycler extends RecyclerView.Adapter<PauseConfigRecycler.ViewHolder> {
    List<GamePauseInfo> gameList;

    public PauseConfigRecycler(List<GamePauseInfo> gameList) {
        this.gameList = gameList;
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtGameName, statusGame,timeEndGame;

        public ViewHolder(View itemView) {
            super(itemView);
            txtGameName = itemView.findViewById(R.id.txtGamePause);
            statusGame = itemView.findViewById(R.id.statusGamePause);
            timeEndGame = itemView.findViewById(R.id.timeEndGamePause);

        }
    }
    @NonNull
    @Override
    public PauseConfigRecycler.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.pause_item_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PauseConfigRecycler.ViewHolder holder, int position) {
        GamePauseInfo game = gameList.get(position);
        holder.txtGameName.setText(game.getName());
        holder.statusGame.setText("Trạng thái: " + game.getStatus());
        holder.timeEndGame.setText("Thời gian kết thúc: " + game.getTimeEnd());
    }
    public void updateGameList(List<GamePauseInfo> newGameList) {
        this.gameList = newGameList;
        notifyDataSetChanged(); // Thông báo cho adapter rằng dữ liệu đã thay đổi
    }
    @Override
    public int getItemCount() {
        return gameList.size();
    }
}