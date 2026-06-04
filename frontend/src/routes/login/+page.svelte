<script lang="ts">
	import { enhance } from "$app/forms";

    let username = $state('');
    let password = $state('');

    let { 
        form = {
        success: undefined,
        error: undefined,
        message: undefined,
        user: undefined
        }
    } = $props<{
        form?: {
        success?: boolean;
        error?: string;
        message?: string;
        user?: {
            username: string;
            firstName: string;
            lastName: string;
            roles: string[];
        };
        };
    }>();
</script>

<main>
    <h1 class="main-header">Макет СПО ПРЦА Электро-М
    </h1>
    <div class="login-container">
        <h2>Авторизация в системе</h2>
        <form method="POST" action="?/login" use:enhance>
            <div class="form-group">
                <label for="username">Имя пользователя</label>
                <input 
                    type="text"
                    id="username"
                    name="username"
                    bind:value={username}
                    required    
                    autocomplete="username"
                    placeholder="Введите логин"
                    class={form?.error ? 'error' : ''}
                >
            </div>
            <div class="form-group">
                <label for="password">Пароль</label>
                <input 
                    type="password"
                    id="password"
                    name="password"
                    bind:value={password}
                    required
                    autocomplete="current-password"
                    placeholder="Введите пароль"
                    class={form?.error ? 'error' : ''}
                >
            </div>
            <button type="submit" class="submit-btn">Войти</button>
        </form>

        {#if form?.error}
            <div class="alert error">
                <strong>Ошибка: </strong>
                {form?.error}
            </div>
        {/if}

    </div>
</main>

<style>
    main {
        position: relative;
        overflow: hidden;
        display: flex;
        justify-content: center;
        align-items: center;
        flex-direction: column;
        /* background: linear-gradient(135deg, #233481 0%, #667eea 100%); */
        background: #667eea;
        padding: 1rem;
        min-height: 100vh;
    }
    .login-container {
        background: white;
        padding: 2.5rem;
        border-radius: 12px;
        box-shadow: 0 10px 40px rgba(0,0,0,0.1);
        min-width: 400px;
        max-width: 400px;
    }

    h1 {
        margin: 0 0 2rem 0;
        text-align: center;
        font-size: 1.5rem;
        width: 80%;
    }

    h2 {
        margin: 0 0 1rem 0;
        color: #333;
        text-align: center;
        font-size: 1.3rem;
    }

    .form-group {
        margin-bottom: 1.5rem;
    }
  
    label {
        display: block;
        margin-bottom: 0.5rem;
        color: #555;
        font-weight: 500;
    }
    
    input {
        width: 100%;
        padding: 0.75rem 1rem;
        border: 1px solid #ddd;
        border-radius: 6px;
        font-size: 1rem;
        transition: border-color 0.2s;
        box-sizing: border-box;
    }
    
    input:focus {
        outline: none;
        border-color: #667eea;
        box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
    }
    
    input.error {
        border-color: #dc3545;
    }
    
    .submit-btn {
        width: 100%;
        padding: 0.875rem;
        background: #667eea;
        color: white;
        border: none;
        border-radius: 6px;
        font-size: 1rem;
        font-weight: 600;
        cursor: pointer;
        transition: background 0.2s;
        margin-top: 0.5rem;
    }
    
    .submit-btn:hover {
        background: #5a67d8;
    }
    
    .submit-btn:disabled {
        background: #ccc;
        cursor: not-allowed;
    }
    
    .alert {
        padding: 1rem;
        border-radius: 6px;
        margin-top: 1.5rem;
        text-align: center;
    }
    
    .alert.error {
        background: #fee;
        border: 1px solid #fcc;
        color: #c00;
    }
    
    .alert.success {
        background: #d4edda;
        border: 1px solid #c3e6cb;
        color: #155724;
    }
</style>