<script lang="ts">
  import type { PageData } from "./$types";
  export let data: PageData;
</script>

<main>
  <div class="profile-container">
    <h1>Профиль пользователя</h1>
    
    {#if data?.user}
      <div class="profile-card">
        <h2>{data.user.firstName} {data.user.lastName}</h2>
        <p class="username">@{data.user.username}</p>
        
        <div class="user-info">
          <p><strong>Роли:</strong> {data.user.roles?.join(', ') || 'Нет ролей'}</p>
          
          {#if data.user.lastLoginAt}
            <p><strong>Последний вход:</strong> 
              {new Date(data.user.lastLoginAt).toLocaleString('ru-RU')}
            </p>
          {/if}
        </div>
        
        <form method="POST" action="/?/logout">
          <button type="submit" class="logout-btn">Выйти</button>
        </form>
      </div>
    {:else}
      <p>Загрузка профиля...</p>
    {/if}
  </div>
</main>

<style>
  main {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    background: #667eea;
    padding: 1rem;
  }
  
  .profile-container {
    background: white;
    padding: 2.5rem;
    border-radius: 12px;
    box-shadow: 0 10px 40px rgba(0,0,0,0.1);
    min-width: 400px;
    max-width: 600px;
  }
  
  h1 {
    text-align: center;
    color: #333;
    margin-bottom: 2rem;
  }
  
  .profile-card {
    text-align: center;
  }
  
  .profile-card h2 {
    color: #444;
    margin: 0 0 0.5rem 0;
  }
  
  .username {
    color: #666;
    font-style: italic;
    margin: 0 0 1.5rem 0;
  }
  
  .user-info {
    background: #f8f9fa;
    padding: 1rem;
    border-radius: 8px;
    margin: 1.5rem 0;
    text-align: left;
  }
  
  .user-info p {
    margin: 0.5rem 0;
  }
  
  .logout-btn {
    background: #dc3545;
    color: white;
    border: none;
    padding: 0.75rem 2rem;
    border-radius: 6px;
    font-size: 1rem;
    cursor: pointer;
    width: 100%;
    margin-top: 1rem;
  }
  
  .logout-btn:hover {
    background: #c82333;
  }
</style>