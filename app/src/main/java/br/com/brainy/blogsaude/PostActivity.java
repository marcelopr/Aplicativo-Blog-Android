package br.com.brainy.blogsaude;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;

public class PostActivity extends AppCompatActivity {

    private ImageButton selecionarImagem;
    private EditText titulo, descricao;
    private Button postar;

    private static int GALLERY_REQUEST = 1;

    private Uri imageUri = null;
    private Uri resultUri = null;

    private StorageReference mStorage;
    private DatabaseReference mDatabase;
    private DatabaseReference mDatabaseUser;
    private FirebaseAuth mAuth;
    private FirebaseUser mCurrentUser;
    private LinearLayout ll_add_imagem_post;

    private ProgressDialog mProgress;

    private Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        toolbar = (Toolbar)findViewById(R.id.tb_postar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");

        ActionBar actionBar = getSupportActionBar();

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setTitle("");

        mAuth = FirebaseAuth.getInstance();
        mCurrentUser = mAuth.getCurrentUser();

        mStorage = FirebaseStorage.getInstance().getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Blog");
        mDatabaseUser = FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUser.getUid());

        mProgress = new ProgressDialog(this);

        ll_add_imagem_post = (LinearLayout)findViewById(R.id.ll_add_imagem_post);
        selecionarImagem = (ImageButton) findViewById(R.id.imageSelect);
        titulo = (EditText) findViewById(R.id.titleField);
        descricao = (EditText) findViewById(R.id.descField);
        postar = (Button) findViewById(R.id.submitBtn);

        ll_add_imagem_post.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent galeria = new Intent();
                galeria.setType("image/*");
                galeria.setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(galeria, "SELECT IMAGE"), GALLERY_REQUEST);

            }
        });

        postar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iniciarPostagem();
            }
        });

    }

    private void iniciarPostagem() {

        mProgress.setMessage("Postando");
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.show();

        final String tituloPost = titulo.getText().toString().trim();
        final String descricaoPost = descricao.getText().toString().trim();

        if (!TextUtils.isEmpty(tituloPost) && !TextUtils.isEmpty(descricaoPost)) {

            StorageReference caminhoArquivo = mStorage.child("Imagens_Blog").child(imageUri.getLastPathSegment());

            caminhoArquivo.putFile(resultUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    final Uri downloadUrl = taskSnapshot.getDownloadUrl();
                    final DatabaseReference novoPost = mDatabase.push();

                    String user_id = mAuth.getCurrentUser().getUid();

                    mDatabaseUser.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            HashMap postMap = new HashMap();
                            postMap.put("title", tituloPost);
                            postMap.put("desc", descricaoPost);
                            postMap.put("uid", mCurrentUser.getUid());
                            postMap.put("image", downloadUrl.toString());
                            postMap.put("username", dataSnapshot.child("name").getValue());

                            novoPost.setValue(postMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {

                                    if (task.isSuccessful()) {
                                        mProgress.dismiss();
                                        finish();
                                    } else {
                                        Toast.makeText(PostActivity.this, "Erro ao postar. Tente novamente!", Toast.LENGTH_LONG).show();
                                    }

                                }
                            });

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });


                }
            });

        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_REQUEST && resultCode == RESULT_OK) {
            imageUri = data.getData();
            CropImage.activity(imageUri).
                    setGuidelines(CropImageView.Guidelines.ON)
                    .setRequestedSize(500, 500).
                    setAspectRatio(1,1).
                    start(this);

        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                resultUri = result.getUri();
                selecionarImagem.setImageURI(resultUri);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }

    }

}
