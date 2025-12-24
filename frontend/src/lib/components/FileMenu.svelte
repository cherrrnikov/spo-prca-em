<script lang="ts">
  import { enhance } from '$app/forms';
  import { goto } from '$app/navigation';
  import type { UserResponse } from '$lib/types/auth';
//   import { scheduleIntervals } from '$lib/stores/scheduleStore';
  
  let isOpen = $state(false);

  let {userData} = $props<{
    userData: UserResponse | null
  }>();

  function handleCreateSchedule() {
    alert('Создание нового ПРЦА');
  }

  function handleSave() {
    // const intervals = scheduleIntervals;
    // console.log('Сохранение интервалов:', intervals);
    alert('ПРЦА сохранен');
  }

  function handleExport() {
    alert('Генерация отчета');
  }

  function handleArchive() {
    goto('/archive');
  }

  function handleLogout() {
    if (confirm('Вы уверены, что хотите выйти?')) {
      goto('/');
    }
  }

  function handleAdmin() {
    goto('/admin');
  }
</script>

<div class="file-menu">
  <button class="menu-button" onclick={() => isOpen = !isOpen}>
    Действия
  </button>

  {#if isOpen}
    <div class="dropdown">
      <div class="dropdown-content">
        <button onclick={handleCreateSchedule} class="menu-item">
          Создать ПРЦА
        </button>
        <button onclick={handleSave} class="menu-item">
          Сохранить ПРЦА
        </button>
        <hr />
        <button onclick={handleExport} class="menu-item">
          Отчет
        </button>
        <button onclick={handleArchive} class="menu-item">
          Просмотр архива
        </button>
        <hr />
                
        <div class="user-info">
            {#if userData}
                <div class="user-details">
                    <div class="user-name">
                        {userData.firstName} {userData.lastName}
                    </div>
                    <div class="user-meta">
                        <span class="username">@{userData.username}</span>
                        {#if userData.roles && userData.roles.length > 0}
                            <span class="user-role">• {userData.roles[0]}</span>
                        {/if}
                    </div>
                </div>
            {:else}
                <div class="user-details">
                    <div class="user-name">
                        Не авторизован
                    </div>
                </div>
            {/if}
        </div>
        {#if userData?.roles?.includes('ADMIN')}
            <button onclick={handleAdmin} type="submit" class="menu-item">Панель админа</button>
        {/if}
        <form method="POST" action="/?/logout" use:enhance>
            <button type="submit" class="menu-item logout">
            Выход
            </button>
        </form>
      </div>
    </div>
  {/if}
</div>

<style>
  .file-menu {
    position: relative;
    z-index: 1000;
  }

  .menu-button {
    background: #4299e1;
    color: white;
    border: none;
    padding: 0.75rem 1.5rem;
    border-radius: 6px;
    cursor: pointer;
    font-size: 1rem;
    font-weight: 500;
    transition: background 0.2s;
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }

  .menu-button:hover {
    background: #3182ce;
  }

  .dropdown {
    position: absolute;
    top: 100%;
    left: 0;
    margin-top: 0.5rem;
  }

  .dropdown-content {
    background: white;
    border-radius: 8px;
    box-shadow: 0 10px 25px rgba(0,0,0,0.1);
    min-width: 220px;
    overflow: hidden;
    border: 1px solid #e2e8f0;
  }

  .menu-item {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    width: 100%;
    padding: 0.875rem 1rem;
    border: none;
    background: none;
    text-align: left;
    cursor: pointer;
    font-size: 0.9rem;
    color: #2d3748;
    transition: background 0.2s;
  }

  .menu-item:hover {
    background: #f7fafc;
  }

  .menu-item.logout {
    color: #e53e3e;
  }

  hr {
    margin: 0;
    border: none;
    border-top: 1px solid #e2e8f0;
  }
  
  .user-info {
      display: flex;
      flex-direction: column;
      padding: 1rem;
      gap: 12px;
  }
  
  .user-details {
      text-align: left;
  }
  
  .user-name {
      font-size: 1rem;
      font-weight: 600;
      color: #2d3748;
      line-height: 1.2;
  }
  
  .user-meta {
      font-size: 0.85rem;
      color: #718096;
      display: flex;
      align-items: center;
      gap: 6px;
      line-height: 1.2;
  }
  
  .username {
      font-weight: 500;
  }
  
  .user-role {
      font-style: italic;
  }
</style>