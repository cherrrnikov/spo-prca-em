<script lang="ts">
  import { goto } from '$app/navigation';
  import { onMount } from 'svelte';
  
  let user = $state<any>(null);
  
  onMount(() => {
    const token = localStorage.getItem('accessToken');
    const userData = localStorage.getItem('user');
    
    if (!token || !userData) {
      goto('/');
      return;
    }
    
    user = JSON.parse(userData);
  });
  
  function logout() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    goto('/');
  }
</script>

{#if user}
  <div>
    <h1>Личный кабинет</h1>
    
    <div>
      <p><strong>Пользователь:</strong> {user.firstName} {user.lastName}</p>
      <p><strong>Логин:</strong> {user.username}</p>
      <p><strong>Роли:</strong> {user.roles?.join(', ')}</p>
    </div>
    
    <button on:click={logout}>Выйти</button>
    
    <div>
      <h3>Данные из localStorage:</h3>
      <pre>{JSON.stringify(user, null, 2)}</pre>
    </div>
  </div>
{:else}
  <p>Загрузка...</p>
{/if}

<style>
  div {
    max-width: 600px;
    margin: 50px auto;
    padding: 20px;
  }
  
  button {
    padding: 10px 20px;
    background: #dc3545;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    margin-top: 20px;
  }
</style>