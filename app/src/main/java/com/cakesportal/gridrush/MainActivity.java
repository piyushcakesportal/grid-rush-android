package com.cakesportal.gridrush;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private GridView game;
    private TextView scoreText, timeText, comboText, statusText;
    private CountDownTimer timer;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        root.setBackgroundColor(Color.rgb(23,21,43));

        TextView title = text("GRID RUSH", 28, Color.WHITE, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap(dp(56)));

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);

        scoreText = stat("0", "SCORE");
        timeText = stat("45", "SECONDS");
        comboText = stat("x1", "COMBO");

        stats.addView(scoreText, weight());
        stats.addView(timeText, weight());
        stats.addView(comboText, weight());
        root.addView(stats, matchWrap(dp(72)));

        statusText = text("Tap any connected group of 3 or more matching tiles.", 15,
                Color.rgb(218,216,235), false);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, dp(8), 0, dp(12));
        root.addView(statusText, matchWrap(dp(64)));

        game = new GridView(this);
        root.addView(game, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        Button restart = new Button(this);
        restart.setText("RESTART GAME");
        restart.setTextSize(16);
        restart.setTextColor(Color.WHITE);
        restart.setBackgroundColor(Color.rgb(99,77,219));
        restart.setOnClickListener(v -> startGame());
        LinearLayout.LayoutParams bp = matchWrap(dp(58));
        bp.setMargins(0, dp(14), 0, 0);
        root.addView(restart, bp);

        setContentView(root);
        startGame();
    }

    private void startGame() {
        if (timer != null) timer.cancel();
        game.reset();
        update();
        statusText.setText("Tap any connected group of 3 or more matching tiles.");

        timer = new CountDownTimer(45000, 1000) {
            public void onTick(long ms) {
                game.seconds = (int)Math.ceil(ms / 1000.0);
                update();
            }
            public void onFinish() {
                game.seconds = 0;
                game.active = false;
                update();
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Time!")
                        .setMessage("Your score: " + game.score)
                        .setPositiveButton("Play again", (d,w) -> startGame())
                        .setNegativeButton("Close", null)
                        .show();
            }
        }.start();
    }

    private void update() {
        scoreText.setText(game.score + "\nSCORE");
        timeText.setText(game.seconds + "\nSECONDS");
        comboText.setText("x" + game.combo + "\nCOMBO");
    }

    private TextView stat(String value, String label) {
        TextView t = text(value + "\n" + label, 19, Color.WHITE, true);
        t.setGravity(Gravity.CENTER);
        t.setBackgroundColor(Color.rgb(43,39,74));
        t.setPadding(dp(4), dp(8), dp(4), dp(8));
        return t;
    }

    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private LinearLayout.LayoutParams weight() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        p.setMargins(dp(4), 0, dp(4), 0);
        return p;
    }

    private LinearLayout.LayoutParams matchWrap(int h) {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, h);
    }

    private int dp(int n) {
        return (int)(n * getResources().getDisplayMetrics().density + .5f);
    }

    private class GridView extends View {
        final int rows=6, cols=6, kinds=5;
        int[][] board = new int[rows][cols];
        int[] colors = {
                Color.rgb(245,88,109),
                Color.rgb(255,188,66),
                Color.rgb(56,189,248),
                Color.rgb(83,210,137),
                Color.rgb(172,111,255)
        };
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Random random = new Random();
        int score=0, combo=1, seconds=45;
        boolean active=true;

        GridView(Activity c) { super(c); setBackgroundColor(Color.rgb(30,27,54)); }

        void reset() {
            score=0; combo=1; seconds=45; active=true;
            for(int r=0;r<rows;r++) for(int c=0;c<cols;c++) board[r][c]=random.nextInt(kinds);
            invalidate();
        }

        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float gap=8f;
            float size=Math.min((getWidth()-gap*(cols+1))/cols,
                    (getHeight()-gap*(rows+1))/rows);
            float ox=(getWidth()-(size*cols+gap*(cols+1)))/2f;
            float oy=(getHeight()-(size*rows+gap*(rows+1)))/2f;
            for(int r=0;r<rows;r++) for(int c=0;c<cols;c++) {
                float l=ox+gap+c*(size+gap), t=oy+gap+r*(size+gap);
                paint.setColor(colors[board[r][c]]);
                canvas.drawRoundRect(l,t,l+size,t+size,size*.22f,size*.22f,paint);
                paint.setColor(0x33FFFFFF);
                canvas.drawCircle(l+size*.33f,t+size*.30f,size*.15f,paint);
            }
        }

        public boolean onTouchEvent(android.view.MotionEvent e) {
            if(e.getAction()!=MotionEvent.ACTION_DOWN || !active) return true;
            float gap=8f;
            float size=Math.min((getWidth()-gap*(cols+1))/cols,
                    (getHeight()-gap*(rows+1))/rows);
            float ox=(getWidth()-(size*cols+gap*(cols+1)))/2f;
            float oy=(getHeight()-(size*rows+gap*(rows+1)))/2f;
            int c=(int)((e.getX()-ox-gap)/(size+gap));
            int r=(int)((e.getY()-oy-gap)/(size+gap));
            if(r<0||r>=rows||c<0||c>=cols) return true;

            boolean[][] seen=new boolean[rows][cols];
            ArrayList<int[]> group=new ArrayList<>();
            flood(r,c,board[r][c],seen,group);

            if(group.size()>=3) {
                for(int[] p:group) board[p[0]][p[1]]=-1;
                score += group.size()*group.size()*10*combo;
                combo=Math.min(combo+1,9);
                collapse();
                statusText.setText("Great! " + group.size() + " tiles cleared.");
                update();
                invalidate();
            } else {
                combo=1;
                statusText.setText("Need at least 3 connected matching tiles.");
                update();
            }
            return true;
        }

        void flood(int r,int c,int type,boolean[][] seen,ArrayList<int[]> out) {
            if(r<0||r>=rows||c<0||c>=cols||seen[r][c]||board[r][c]!=type) return;
            seen[r][c]=true; out.add(new int[]{r,c});
            flood(r-1,c,type,seen,out); flood(r+1,c,type,seen,out);
            flood(r,c-1,type,seen,out); flood(r,c+1,type,seen,out);
        }

        void collapse() {
            for(int c=0;c<cols;c++) {
                int write=rows-1;
                for(int r=rows-1;r>=0;r--) if(board[r][c]!=-1) board[write--][c]=board[r][c];
                while(write>=0) board[write--][c]=random.nextInt(kinds);
            }
        }
    }
}
