const productsElement = document.getElementById('products');
const messageElement = document.getElementById('message');
const form = document.getElementById('productForm');


async function loadProducts() {
    const response = await fetch('/api/products');

    if (!response.ok) {
        throw new Error('Не удалось загрузить');
    }

    const products = await response.json();
    productsElement.replaceChildren();

    if (products.length === 0) {
        productsElement.textContent = 'Список пока пуст.';
        return;
    }

    products.forEach(product => {
        const card = document.createElement('div');
        card.className = 'product';

        const row = document.createElement('div');
        row.className = 'row';

        const description = document.createElement('span');
        description.textContent =
            `${product.name} — ${product.quantity} ${product.unit || ''}`;

        const actions = document.createElement('div');
        actions.className = 'product-actions';

        const editButton = document.createElement('button');
        editButton.textContent = 'Изменить';

        const deleteButton = document.createElement('button');
        deleteButton.className = 'delete';
        deleteButton.textContent = 'Удалить';

        const editForm = document.createElement('form');
        editForm.hidden = true;

        const fields = document.createElement('div');
        fields.className = 'edit-fields';

        const nameInput = document.createElement('input');
        nameInput.value = product.name;
        nameInput.maxLength = 100;
        nameInput.required = true;
        nameInput.placeholder = 'Название';
        nameInput.setAttribute('aria-label', 'Название продукта');

        const quantityInput = document.createElement('input');
        quantityInput.type = 'number';
        quantityInput.min = '1';
        quantityInput.step = '1';
        quantityInput.required = true;
        quantityInput.value = product.quantity;
        quantityInput.placeholder = 'Количество';
        quantityInput.setAttribute('aria-label', 'Количество');

        const unitSelect = document.createElement('select');
        unitSelect.setAttribute('aria-label', 'Единица измерения');

        [
            ['шт.', 'Штуки'],
            ['г', 'Граммы'],
            ['кг', 'Килограммы'],
            ['мл', 'Миллилитры'],
            ['л', 'Литры']
        ].forEach(([value, label]) => {
            const option = document.createElement('option');
            option.value = value;
            option.textContent = label;
            unitSelect.appendChild(option);
        });

        unitSelect.value = product.unit || 'шт.';

        const saveButton = document.createElement('button');
        saveButton.type = 'submit';
        saveButton.textContent = 'Сохранить';

        const cancelButton = document.createElement('button');
        cancelButton.type = 'button';
        cancelButton.className = 'cancel';
        cancelButton.textContent = 'Отмена';

        const editMessage = document.createElement('p');
        editMessage.setAttribute('role', 'status');

        fields.append(nameInput, quantityInput, unitSelect);
        editForm.append(fields, saveButton, cancelButton, editMessage);

        editButton.addEventListener('click', () => {
            editForm.hidden = false;
            editButton.hidden = true;
            nameInput.focus();
        });

        cancelButton.addEventListener('click', () => {
            editForm.hidden = true;
            editButton.hidden = false;

            nameInput.value = product.name;
            quantityInput.value = product.quantity;
            unitSelect.value = product.unit || 'шт.';
            editMessage.textContent = '';
        });

        editForm.addEventListener('submit', async event => {
            event.preventDefault();

            const updatedProduct = {
                name: nameInput.value.trim(),
                quantity: Number(quantityInput.value),
                unit: unitSelect.value
            };

            if (!updatedProduct.name ||
                updatedProduct.quantity <= 0 ||
                !Number.isInteger(updatedProduct.quantity)) {
                editMessage.textContent =
                    'Введи название и положительное целое количество.';
                return;
            }

            saveButton.disabled = true;
            editMessage.textContent = 'Сохраняю...';

            try {
                const result = await fetch(
                    `/api/products/${product.id}`,
                    {
                        method: 'PUT',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify(updatedProduct)
                    }
                );

                if (!result.ok) {
                    throw new Error(
                        result.status === 404
                            ? 'Продукт не найден.'
                            : 'Не удалось сохранить изменения.'
                    );
                }

                await loadProducts();
                messageElement.textContent = 'Продукт обновлён!';
            } catch (error) {
                editMessage.textContent = error.message;
            } finally {
                saveButton.disabled = false;
            }
        });

        deleteButton.addEventListener('click', async () => {
            try {
                const result = await fetch(
                    `/api/products/${product.id}`,
                    { method: 'DELETE' }
                );

                if (!result.ok) {
                    throw new Error('Не удалось удалить продукт.');
                }

                await loadProducts();
                messageElement.textContent = 'Продукт удалён.';
            } catch (error) {
                messageElement.textContent = error.message;
            }
        });

        actions.append(editButton, deleteButton);
        row.append(description, actions);
        card.append(row, editForm);
        productsElement.append(card);
    });
}


form.addEventListener('submit', async event => {
    event.preventDefault();
    messageElement.textContent = '';

    const product = {
        name: document.getElementById('name').value.trim(),
        quantity: Number(document.getElementById('quantity').value),
        unit: document.getElementById('unit').value.trim()
    };

    try {
        const response = await fetch('/api/products', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(product)
        });

        if (!response.ok) {
            throw new Error('Не удалось добавить');
        }

        form.reset();
        messageElement.textContent = 'Продукт добавлен!';
        await loadProducts();
    } catch (error) {
        messageElement.textContent = error.message;
    }
});

loadProducts().catch(() => {
    productsElement.textContent = 'Ошибка загрузки.';
});


let parsedCommandProducts = [];

async function parseProductCommand() {
    const command = document.getElementById("productCommand").value.trim();
    const status = document.getElementById("commandStatus");
    const result = document.getElementById("parsedProducts");
    const saveButton = document.getElementById("saveCommandProducts");

    if (!command) {
        status.textContent = "Сначала введи команду.";
        return;
    }

    saveButton.hidden = true;
    result.innerHTML = "";
    parsedCommandProducts = [];
    status.textContent = "ИИ разбирает команду...";

    try {
        const response = await fetch("/api/products/command/parse", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ command: command })
        });

        if (!response.ok) {
            throw new Error("Не удалось разобрать команду");
        }

        const data = await response.json();
        parsedCommandProducts = data.products;

        if (!Array.isArray(parsedCommandProducts) ||
            parsedCommandProducts.length === 0) {
            status.textContent = "ИИ не нашла продуктов в команде.";
            return;
        }

        const list = document.createElement("ul");

        parsedCommandProducts.forEach((product, index) => {
            const item = document.createElement("li");
            item.textContent =
                `${product.name} — ${product.quantity} ${product.unit}`;
            list.appendChild(item);
        });

        result.appendChild(list);
        status.textContent = "Проверь список перед сохранением.";
        saveButton.hidden = false;
    } catch (error) {
        status.textContent = "Ошибка: " + error.message;
    }
}

async function saveCommandProducts() {
    const status = document.getElementById("commandStatus");
    const saveButton = document.getElementById("saveCommandProducts");

    if (parsedCommandProducts.length === 0) {
        status.textContent = "Сначала разбери команду.";
        return;
    }

    saveButton.disabled = true;
    status.textContent = "Сохраняю продукты...";

    try {
        const response = await fetch("/api/products/command/save", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(parsedCommandProducts)
        });

        if (!response.ok) {
            throw new Error("Не удалось сохранить продукты");
        }

        const data = await response.json();

        status.textContent = `Сохранено продуктов: ${data.saved}.`;
        parsedCommandProducts = [];
        document.getElementById("parsedProducts").innerHTML = "";
        saveButton.hidden = true;
        document.getElementById("productCommand").value = "";

        if (typeof loadProducts === "function") {
            await loadProducts();
        }
    } catch (error) {
        status.textContent = "Ошибка: " + error.message;
    } finally {
        saveButton.disabled = false;
    }
}

const recipeButton = document.getElementById('recipeButton');
const recipeMessage = document.getElementById('recipeMessage');
const recipeResult = document.getElementById('recipeResult');
const recipeLoading = document.getElementById('recipe-loading');

recipeButton.addEventListener('click', async () => {
    recipeButton.disabled = true;
    recipeMessage.textContent = '';
    recipeResult.textContent = '';
    recipeLoading.hidden = false;

    requestAnimationFrame(() => {
        recipeLoading.scrollIntoView({
            behavior: "smooth",
            block: "end"
        });
    });

    const dishType = document.getElementById('dishType').value;
    const cuisine = document.getElementById('cuisine').value;
    const preferences = document.getElementById('preferences').value.trim();

    const params = new URLSearchParams();

    if (dishType) {
        params.set('dishType', dishType);
    }

    if (cuisine) {
        params.set('cuisine', cuisine);
    }

    if (preferences) {
        params.set('preferences', preferences);
    }

    try {
        const response = await fetch(
            '/api/recipes/suggest?' + params.toString()
        );

        if (!response.ok) {
            throw new Error('Не удалось получить рецепт');
        }

        const data = await response.json();

        recipeResult.textContent = data.recipe;
    } catch (error) {
        recipeMessage.textContent =
            'Ошибка при получении рецепта. Проверь, запущен ли Ollama.';
    } finally {
        recipeLoading.hidden = true;
        recipeButton.disabled = false;
    }
});

let voiceRecognition = null;
let voicePointerId = null;
let voiceShouldStop = false;
let voiceTarget = null;
let voiceFinalText = "";
let voiceBaseText = "";

function startVoiceInput(textareaId, statusId, event) {
    if (voiceRecognition) return;

    const SpeechRecognition =
        window.SpeechRecognition ||
        window.webkitSpeechRecognition;

    const textarea = document.getElementById(textareaId);
    const status = document.getElementById(statusId);
    const button = event.currentTarget;

    if (!SpeechRecognition) {
        status.textContent =
            "Распознавание речи не поддерживается этим браузером.";
        return;
    }

    event.preventDefault();

    voicePointerId = event.pointerId;
    voiceTarget = { textarea, status, button };
    voiceShouldStop = false;
    voiceFinalText = "";
    voiceBaseText = textarea.value.trim();

    const recognition = new SpeechRecognition();
    voiceRecognition = recognition;

    recognition.lang = "ru-RU";
    recognition.continuous = true;
    recognition.interimResults = true;

    button.classList.add("recording");
    status.textContent = "🔴 Слушаю... Отпусти кнопку, когда закончишь.";

    recognition.onresult = function(event) {
        let finalTranscript = "";
        let interimTranscript = "";

        for (let i = 0; i < event.results.length; i++) {
            const result = event.results[i];
            const transcript = result[0].transcript;

            if (result.isFinal) {
                finalTranscript += transcript + " ";
            } else {
                interimTranscript += transcript;
            }
        }

        voiceFinalText = finalTranscript.trim();

        const parts = [voiceBaseText, voiceFinalText, interimTranscript.trim()]
            .filter(Boolean);

        textarea.value = parts.join(" ");
    };

    recognition.onerror = function(event) {
        if (voiceTarget) {
            voiceTarget.status.textContent =
                "Ошибка распознавания: " + event.error;
        }
    };

    recognition.onend = function() {
        const target = voiceTarget;

        if (target) {
            target.button.classList.remove("recording");

            const parts = [voiceBaseText, voiceFinalText].filter(Boolean);
            target.textarea.value = parts.join(" ");

            if (voiceShouldStop) {
                target.status.textContent = voiceFinalText
                    ? "Речь распознана."
                    : "Речь не распознана. Попробуй ещё раз.";
            }
        }

        voiceRecognition = null;
        voicePointerId = null;
        voiceTarget = null;
        voiceShouldStop = false;
    };

    try {
        recognition.start();
        button.setPointerCapture(event.pointerId);
    } catch (error) {
        button.classList.remove("recording");
        status.textContent = "Не удалось запустить микрофон.";
        voiceRecognition = null;
        voiceTarget = null;
        voicePointerId = null;
    }
}

function stopVoiceInput(event) {
    if (!voiceRecognition) return;

    if (event && voicePointerId !== event.pointerId) return;

    voiceShouldStop = true;

    try {
        voiceRecognition.stop();
    } catch (error) {
    }
}
