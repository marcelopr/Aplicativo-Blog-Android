package br.com.brainy.blogsaude;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    private EditText nome, email, senha;
    private ImageButton cadastrar;
    private FirebaseAuth mAuth;
    private ProgressDialog mProgress;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mProgress = new ProgressDialog(this);

        mDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

        nome = (EditText)findViewById(R.id.registrar_nome);
        email = (EditText)findViewById(R.id.registrar_email);
        senha = (EditText)findViewById(R.id.registrar_senha);
        cadastrar = (ImageButton)findViewById(R.id.btn_registrar);

        cadastrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRegister();
            }
        });
    }

    private void startRegister() {

        final String nomeUsuario = nome.getText().toString().trim();
        final String emailUsuario = email.getText().toString().trim();
        String senhaUsuario = senha.getText().toString().trim();

        if( !TextUtils.isEmpty(nomeUsuario) && !TextUtils.isEmpty(emailUsuario) && !TextUtils.isEmpty(senhaUsuario) ){

            mProgress.setMessage("Cadastrando");
            mProgress.setCanceledOnTouchOutside(false);
            mProgress.show();

            mAuth.createUserWithEmailAndPassword(emailUsuario, senhaUsuario).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {

                    if(task.isSuccessful()){

                        String user_id = mAuth.getCurrentUser().getUid();

                        HashMap<String, String> cadastroMap = new HashMap<>();
                        cadastroMap.put("name", nomeUsuario);
                        cadastroMap.put("image", "default");
                        cadastroMap.put("email", emailUsuario);

                        mDatabase.child(user_id).setValue(cadastroMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

                                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);

                                mProgress.dismiss();

                            }
                        });


                    }else{
                        Toast.makeText(RegisterActivity.this, "Não foi possível realizar o cadastro no momento. Por favor, tente novamente mais tarde!", Toast.LENGTH_SHORT).show();
                        mProgress.dismiss();
                    }

                }
            });

        }

    }
}
