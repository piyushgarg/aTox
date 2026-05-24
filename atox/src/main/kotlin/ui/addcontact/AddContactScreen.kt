package ltd.evilcorp.atox.ui.addcontact

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import ltd.evilcorp.atox.R
import ltd.evilcorp.core.tox.ToxID

import androidx.compose.ui.platform.LocalSoftwareKeyboardController

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
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        viewModel.uiEvents
            .flowWithLifecycle(lifecycleOwner.lifecycle)
            .collectLatest { event ->
                when (event) {
                    is AddContactViewModel.AddContactUiEvent.Success -> {
                        keyboardController?.hide()
                        onSuccess()
                    }
                    is AddContactViewModel.AddContactUiEvent.ShowError -> {
                        keyboardController?.hide()
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
            keyboardController?.hide()
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = stringResource(R.string.add_contact_send_request),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

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
                            { Text(errorText) }
                        } else null,
                        singleLine = true,
                        enabled = !isLoading,
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.fillMaxWidth()
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
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { submitContactRequest() },
                        enabled = !isLoading && toxIdInput.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(R.string.add), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showBackButton) {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.add_contact), fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack, enabled = !isLoading) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
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
