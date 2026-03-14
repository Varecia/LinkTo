package com.tos.linkto.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tos.linkto.MainActivity
import com.tos.linkto.R

@Composable
fun LinkScreen(){
    if(MainActivity.instance.authRepo.isLoggedIn()){

    }else{

    }
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.height(80.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_account_box),
                    contentDescription = "Avatar",
                    modifier = Modifier.padding(vertical = 4.dp).fillMaxSize()
                )
            }
            Text(
                text = stringResource(R.string.placeholder),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Left
            )
            Row(
                modifier = Modifier.height(80.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_account_box),
                    contentDescription = stringResource(R.string.placeholder),
                    modifier = Modifier.padding(vertical = 4.dp).fillMaxSize()
                )
            }
            Row(
                modifier = Modifier.height(80.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_account_box),
                    contentDescription = stringResource(R.string.placeholder),
                    modifier = Modifier.padding(vertical = 4.dp).fillMaxSize()
                )
            }
            Row(
                modifier = Modifier.height(80.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_account_box),
                    contentDescription = stringResource(R.string.placeholder),
                    modifier = Modifier.padding(vertical = 4.dp).fillMaxSize()
                )
            }
            Text(
                text = stringResource(R.string.placeholder),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}