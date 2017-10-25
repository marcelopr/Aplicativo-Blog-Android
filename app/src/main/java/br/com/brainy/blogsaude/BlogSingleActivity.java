package br.com.brainy.blogsaude;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

public class BlogSingleActivity extends AppCompatActivity {

    private String post_key = null;

    private DatabaseReference mDatabase;
    private DatabaseReference mDatabaseLikes;

    private TextView mBlogSingleDesc, mBlogSingleTitle;
    private ImageView mBlogSingleImage;
    private Button mSingleRemoveBtn;

    private FirebaseAuth mAuth;

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blog_single);

        toolbar = (Toolbar)findViewById(R.id.tb_post);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");

        ActionBar actionBar = getSupportActionBar();

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setTitle("");


        mAuth = FirebaseAuth.getInstance();

        mDatabase = FirebaseDatabase.getInstance().getReference().child("Blog");
        mDatabaseLikes = FirebaseDatabase.getInstance().getReference().child("Likes");

        post_key = getIntent().getExtras().getString("blog_id");

        mBlogSingleDesc = (TextView)findViewById(R.id.singleBlogDesc);
        mBlogSingleTitle = (TextView)findViewById(R.id.singleBlogTitle);
        mBlogSingleImage = (ImageView)findViewById(R.id.singleBlogImage);
        mSingleRemoveBtn = (Button) findViewById(R.id.singleRemoveBtn);

        mDatabase.child(post_key).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                String post_title = dataSnapshot.child("title").getValue().toString();
                String post_desc = dataSnapshot.child("desc").getValue().toString();
                String post_image = dataSnapshot.child("image").getValue().toString();
                String post_uid = dataSnapshot.child("uid").getValue().toString();

                mBlogSingleTitle.setText(post_title);
                mBlogSingleDesc.setText(post_desc);
                Picasso.with(BlogSingleActivity.this).load(post_image).into(mBlogSingleImage);

                if( mAuth.getCurrentUser().getUid().equals(post_uid) ){
                    mSingleRemoveBtn.setVisibility(View.VISIBLE);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mSingleRemoveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mDatabase.child(post_key).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        if(task.isSuccessful() ){

                            mDatabaseLikes.child(post_key).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {

                                    if( task.isSuccessful() ) {

                                        Toast.makeText(BlogSingleActivity.this, "Post deletado.", Toast.LENGTH_SHORT).show();
                                        Intent mainIntent = new Intent(BlogSingleActivity.this, MainActivity.class);
                                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(mainIntent);

                                    }else{

                                        Toast.makeText(BlogSingleActivity.this, "Post deletado.", Toast.LENGTH_SHORT).show();
                                        Intent mainIntent = new Intent(BlogSingleActivity.this, MainActivity.class);
                                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(mainIntent);

                                    }
                                }
                            });

                        }else{

                            Toast.makeText(BlogSingleActivity.this, "Ocorreu um erro ao excluir o post.", Toast.LENGTH_SHORT).show();

                        }

                    }
                });


            }
        });

    }
}
