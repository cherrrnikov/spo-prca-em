<script lang="ts">
    import { goto } from '$app/navigation';

  let username = '';
  let password = '';
  let error = '';
  let isLoading = false;
  
  const API_URL = 'http://localhost:8080/api/auth/login';
  
  async function handleLogin(event: Event) {

    event.preventDefault();
    
    if (!username || !password) {
      error = 'Заполните все поля';
      return;
    }
    
    isLoading = true;
    error = '';
    
    try {
      console.log('Отправляем данные на бэкенд...');
      
      const response = await fetch(API_URL, {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        },
        body: JSON.stringify({
          username: username,
          password: password
        })
      });
      
      console.log('Статус ответа:', response.status);
      
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Ошибка ${response.status}: ${errorText}`);
      }
      
      const data = await response.json();
      console.log('Успешный ответ:', data);
      
      localStorage.setItem('accessToken', data.accessToken);
      localStorage.setItem('refreshToken', data.refreshToken);
      localStorage.setItem('user', JSON.stringify({
        username: data.username,
        firstName: data.firstName,
        lastName: data.lastName,
        roles: data.roles
      }));
      
      alert(`Добро пожаловать, ${data.firstName} ${data.lastName}!`);
      
      username = '';
      password = '';
      
      goto("/dashboard");
      
    } catch (err) {
      console.error('Ошибка входа:', err);
      error = err.message.includes('401') 
        ? 'Неверный логин или пароль' 
        : 'Ошибка соединения с сервером';
    } finally {
      isLoading = false;
    }
  }
</script>

<div>
  <h1>Вход в систему</h1>
  
  {#if error}
    <div style="color: red; padding: 10px; background: #fee; border: 1px solid red;">
      ⚠️ {error}
    </div>
  {/if}
  
  <form on:submit={handleLogin}>
    <div>
      <label>Логин:</label>
      <input
        type="text"
        bind:value={username}
        placeholder="Введите логин"
        disabled={isLoading}
      />
    </div>
    
    <div>
      <label>Пароль:</label>
      <input
        type="password"
        bind:value={password}
        placeholder="Введите пароль"
        disabled={isLoading}
      />
    </div>
    
    <button type="submit" disabled={isLoading}>
      {#if isLoading}
        Вход...
      {:else}
        Войти
      {/if}
    </button>
  </form>
  
  <div>
    <p>Для теста используй:</p>
    <p><strong>Логин:</strong> admin</p>
    <p><strong>Пароль:</strong> admin123</p>
  </div>
</div>

<style>
  div {
    max-width: 400px;
    margin: 50px auto;
    padding: 20px;
    border: 1px solid #ccc;
    border-radius: 8px;
  }
  
  h1 {
    text-align: center;
  }
  
  label {
    display: block;
    margin: 10px 0 5px;
  }
  
  input {
    width: 100%;
    padding: 8px;
    margin-bottom: 15px;
    box-sizing: border-box;
  }
  
  button {
    width: 100%;
    padding: 10px;
    background: #007bff;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
  }
  
  button:disabled {
    background: #ccc;
    cursor: not-allowed;
  }
</style>