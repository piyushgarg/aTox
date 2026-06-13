package ltd.evilcorp.atox.ui.addcontact

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import ltd.evilcorp.atox.ui.common.FormErrorText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import ltd.evilcorp.atox.R
import ltd.evilcorp.domain.core.network.ToxID
import ltd.evilcorp.atox.ui.common.AtoxLoadingButton

private const val PADDING_STANDARD = 16

@Composable
fun AddContactScreen(
    viewModel: AddContactViewModel,
    initialToxId: String = "",
    showBackButton: Boolean = true,
    onBack: () -> Unit = {},
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val errorResId by viewModel.errorResId.collectAsState()
    val errorText = errorResId?.let { context.getString(it) }.orEmpty()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        viewModel.uiEvents
            .flowWithLifecycle(lifecycleOwner.lifecycle)
            .collectLatest { event ->
                when (event) {
                    is AddContactViewModel.AddContactUiEvent.Success -> {
                        onSuccess()
                    }
                    is AddContactViewModel.AddContactUiEvent.ShowError -> {
                        Toast.makeText(context, context.getString(event.errorResId), Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    AddContactContent(
        initialToxId = initialToxId,
        showBackButton = showBackButton,
        isLoading = isLoading,
        errorText = errorText,
        onErrorTextChanged = { /* No-op, managed by VM errorState */ },
        onBack = onBack,
        onAddContact = { toxIdStr, message ->
            viewModel.addContact(toxIdStr, message)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactContent(
    initialToxId: String = "",
    showBackButton: Boolean = true,
    isLoading: Boolean = false,
    errorText: String = "",
    onErrorTextChanged: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onAddContact: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var toxIdInput by remember { mutableStateOf(initialToxId) }
    var messageInput by remember {
        mutableStateOf(context.getString(R.string.add_contact_message_default))
    }

    val submitContactRequest = {
        onAddContact(toxIdInput, messageInput)
    }

    val addContactContent = @Composable { paddingValues: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(PADDING_STANDARD.dp),
            verticalArrangement = Arrangement.spacedBy(PADDING_STANDARD.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(PADDING_STANDARD.dp), verticalArrangement = Arrangement.spacedBy(PADDING_STANDARD.dp)) {
                    Text(
                        text = stringResource(R.string.add_contact_send_request),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val isToxIdValid = remember(toxIdInput) { ToxID.isValid(toxIdInput) }

                    OutlinedTextField(
                        value = toxIdInput,
                        onValueChange = {
                            toxIdInput = it
                            if (it.isNotEmpty()) onErrorTextChanged("")
                        },
                        label = { Text(stringResource(R.string.add_contact_friend_id_label)) },
                        placeholder = { Text(stringResource(R.string.add_contact_friend_id_placeholder)) },
                        isError = errorText.isNotEmpty(),
                        supportingText = if (errorText.isNotEmpty()) {
                            { FormErrorText(errorText) }
                        } else null,
                        singleLine = true,
                        enabled = !isLoading,
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isToxIdValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            unfocusedBorderColor = if (isToxIdValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        ),
                        trailingIcon = if (isToxIdValid) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else null,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Tox ID Input" }
                    )

                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        label = { Text(stringResource(R.string.add_contact_message_label)) },
                        singleLine = true,
                        enabled = !isLoading,
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            autoCorrectEnabled = true,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { submitContactRequest() }
                        ),
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Message Input" }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    AtoxLoadingButton(
                        onClick = { submitContactRequest() },
                        text = stringResource(R.string.add),
                        isLoading = isLoading,
                        enabled = isToxIdValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = MaterialTheme.shapes.medium
                    )
                }
            }
        }
    }

    if (showBackButton) {
        val titleString = stringResource(R.string.add_contact)
        val containerColor = MaterialTheme.colorScheme.surfaceContainer
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text(titleString, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack, enabled = !isLoading) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.navigation_back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = containerColor
                    )
                )
            }
        ) { paddingValues ->
            addContactContent(paddingValues)
        }
    } else {
        addContactContent(PaddingValues(0.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun AddContactPreview() {
    MaterialTheme {
        AddContactContent(
            initialToxId = "1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF12345678",
            showBackButton = true,
            isLoading = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddContactLoadingPreview() {
    MaterialTheme {
        AddContactContent(
            initialToxId = "1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF12345678",
            showBackButton = true,
            isLoading = true
        )
    }
}

