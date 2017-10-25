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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private EditText loginEmail, loginPassword;
    private TextView tv_criar_conta;
    private Button btn_logar;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabaseUsers;

    private ProgressDialog mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mDatabaseUsers = FirebaseDatabase.getInstance().getReference().child("Users");
        //Offline
        mDatabaseUsers.keepSynced(true);

        mAuth = FirebaseAuth.getInstance();

        mProgress = new ProgressDialog(this);

        loginEmail = (EditText)findViewById(R.id.loginEmail);
        loginPassword = (EditText)findViewById(R.id.loginPassword);
        tv_criar_conta = (TextView) findViewById(R.id.tv_criar_conta);
        btn_logar = (Button) findViewById(R.id.btn_logar);

        btn_logar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                checkLogin();

            }
        });

        tv_criar_conta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                criarConta();
            }
        });
    }

    private void checkLogin() {

        String email = loginEmail.getText().toString().trim();
        String senha = loginPassword.getText().toString().trim();
        
        if( !TextUtils.isEmpty(email)&& !TextUtils.isEmpty(senha)){

            mAuth.signInWithEmailAndPassword(email, senha).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if( task.isSuccessful() ){

                        checkUserExist();

                    }else{
                        Toast.makeText(LoginActivity.this, "Ocorreu um erro ao entrar. Por favor, tente novamente mais tarde!", Toast.LENGTH_LONG).show();
                    }
                }
            });
            
        }else{
            Toast.makeText(this, "Preencha os campos Email e senha!", Toast.LENGTH_SHORT).show();
        }

    }

    private void checkUserExist() {

        mProgress.setMessage("Entrando");
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.show();

        final String user_id = mAuth.getCurrentUser().getUid();

        mDatabaseUsers.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if(dataSnapshot.hasChild(user_id)){

                    mProgress.dismiss();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);

                }else{
                    Toast.makeText(LoginActivity.this, "Você precisa criar uma conta!", Toast.LENGTH_LONG).show();
                    mProgress.dismiss();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void criarConta() {

        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);

    }
}
