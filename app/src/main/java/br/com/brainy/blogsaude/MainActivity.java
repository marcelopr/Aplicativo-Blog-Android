package br.com.brainy.blogsaude;

import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ButtonBarLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import org.w3c.dom.Text;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    private RecyclerView mBlogList;

    private DatabaseReference mDatabase;
    private DatabaseReference mDatabaseUsers;
    private DatabaseReference mDatabaseLike;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private boolean mProcessLike = false;

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar)findViewById(R.id.tb_feed);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");


        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("");

        mAuth = FirebaseAuth.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if( firebaseAuth.getCurrentUser()==null){
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
            }
        };

        mDatabase = FirebaseDatabase.getInstance().getReference().child("Blog");
        mDatabaseUsers = FirebaseDatabase.getInstance().getReference().child("Users");
        mDatabaseLike = FirebaseDatabase.getInstance().getReference().child("Likes");

        mDatabase.keepSynced(true);
        mDatabaseLike.keepSynced(true);
        mDatabaseUsers.keepSynced(true);

        mBlogList = (RecyclerView)findViewById(R.id.blog_list);
        mBlogList.setLayoutManager(new LinearLayoutManager(this));

       // checkUserExist();

    }

    private void checkUserExist() {

        final String user_id = mAuth.getCurrentUser().getUid();

        mDatabaseUsers.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if( !dataSnapshot.hasChild(user_id) ){

                    Intent intent = new Intent(MainActivity.this, SetupActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);

                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

        mAuth.addAuthStateListener(mAuthListener);

        FirebaseRecyclerAdapter<Post, PostViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Post, PostViewHolder>(
                Post.class,
                R.layout.blog_row,
                PostViewHolder.class,
                mDatabase
        ) {
            @Override
            protected void populateViewHolder(final PostViewHolder viewHolder, final Post model, final int position) {

                final String post_key = getRef(position).getKey();

                viewHolder.setTitle(model.getTitle() );
               // viewHolder.setDesc(model.getDesc() );
                viewHolder.setUsername(model.getUsername());
                viewHolder.setImage(getApplicationContext(), model.getImage() );
                viewHolder.setLikeBtn(post_key);
                viewHolder.setUserImage( MainActivity.this, model.getUid() );
                viewHolder.setNumberOfLikes(post_key);

                //foto
                mDatabaseUsers.child(model.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        viewHolder.setUserImage( MainActivity.this, dataSnapshot.child("image").getValue().toString() );

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                viewHolder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        Intent intent = new Intent(MainActivity.this, BlogSingleActivity.class);
                        intent.putExtra("blog_id", post_key);
                        startActivity(intent);

                    }
                });

                viewHolder.ll_usuario_post.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        abrirPerfil(model.getUid());
                    }
                });

                viewHolder.mLikebtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        mProcessLike = true;

                        mDatabaseLike.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {

                                if(mProcessLike) {
                                    if (dataSnapshot.child(post_key).hasChild(mAuth.getCurrentUser().getUid())) {
                                        //Excluíndo like se ja existe!
                                        mDatabaseLike.child(post_key).child(mAuth.getCurrentUser().getUid()).removeValue();

                                        mProcessLike = false;
                                    } else {
                                        //Colocando nó Id do Usuário dentro da tabela likes daquele post_key
                                        mDatabaseLike.child(post_key).child(mAuth.getCurrentUser().getUid()).setValue("Random");

                                        mProcessLike = false;
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                });

            }
        };

        mBlogList.setAdapter(firebaseRecyclerAdapter);

    }

    public static class PostViewHolder extends RecyclerView.ViewHolder{

        View mView;

        ImageButton mLikebtn;
        DatabaseReference mDatabaseLike;
        FirebaseAuth mAuth;
        LinearLayout ll_usuario_post;

        public PostViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
            mLikebtn = (ImageButton)mView.findViewById(R.id.like_btn);
            ll_usuario_post = (LinearLayout)mView.findViewById(R.id.ll_usuario_post);
            mDatabaseLike = FirebaseDatabase.getInstance().getReference().child("Likes");
            mDatabaseLike.keepSynced(true);
            mAuth = FirebaseAuth.getInstance();

        }

        public void setLikeBtn(final String post_key){

            mDatabaseLike.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    if( dataSnapshot.child(post_key).hasChild(mAuth.getCurrentUser().getUid()) ){
                        mLikebtn.setImageResource(R.drawable.ic_action_ic_thumb_up_blue);
                    }
                    else{
                        mLikebtn.setImageResource(R.mipmap.ic_action_ic_thumb_up_black);
                    }

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        }

        public void setNumberOfLikes(String idPost){

            final TextView post_likes = (TextView)mView.findViewById(R.id.post_likeNumber);

            mDatabaseLike.child(idPost).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    if ( !dataSnapshot.exists() ){
                        post_likes.setText("0");
                    }else{
                        post_likes.setText( ""+dataSnapshot.getChildrenCount() );
                    }

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        }

        public void setTitle(String titulo){
            TextView post_title = (TextView)mView.findViewById(R.id.post_title);
            post_title.setText(titulo);
        }

        public void setDesc(String descricao){
            TextView post_desc = (TextView)mView.findViewById(R.id.post_desc);
            post_desc.setText(descricao+"...");
        }

        public void setUsername(String username){
            TextView post_username = (TextView)mView.findViewById(R.id.post_username);
            post_username.setText(username);
        }

        public void setUserImage(Context context, String userImage){

            final ImageView post_imageUser = (ImageView)mView.findViewById(R.id.post_imageUser);
            Picasso.with(context).load(userImage).into(post_imageUser);

        }

        public void setImage(final Context context, final String imagem){
            final ImageView post_image = (ImageView)mView.findViewById(R.id.post_image);

            Picasso.with(context).load(imagem).into(post_image);
        }

    }

    private void abrirPerfil(String id){

        final AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
        final View mView = this.getLayoutInflater().inflate(R.layout.dialog_perfil, null);

        final CircleImageView iv_foto = (CircleImageView) mView.findViewById(R.id.perfil_imagem);
        final TextView tv_nome = (TextView)mView.findViewById(R.id.perfil_nome);
        final TextView tv_formacao = (TextView)mView.findViewById(R.id.perfil_formacao);

        mDatabaseUsers.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                tv_nome.setText( (String)dataSnapshot.child("name").getValue() );
                tv_formacao.setText( (String)dataSnapshot.child("formation").getValue() );

                if( !dataSnapshot.child("image").toString().equals("default") ){
                    Picasso.with(getApplicationContext()).load( (String)dataSnapshot.child("image").getValue() ).into(iv_foto);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mBuilder.setView(mView);
        final AlertDialog dialog = mBuilder.create();

        dialog.show();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){

            case R.id.action_add:
                startActivity(new Intent(this, PostActivity.class));
                break;
            case R.id.action_profile:
                startActivity(new Intent(this, SetupActivity.class));
                break;
            case R.id.action_logout:
                logout();
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    private void logout() {

        mAuth.signOut();

    }
}
