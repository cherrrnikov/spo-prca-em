<script lang="ts">
  import Modal from '$lib/components/Modal.svelte';
  import { modal } from '$lib/services/modal.service';
  import { onDestroy, onMount } from 'svelte';
  import '../app.css';
  
  let { children } = $props();
  let refreshInterval: ReturnType<typeof setInterval> | null = null;
  let isRefreshing = false;
  
  // Рассчитываем интервал обновления (обновляем каждые 13 минут, но токен живет 15)
  const REFRESH_INTERVAL = 2 * 60 * 1000; // 13 минут
  
  async function refreshSession() {
    if (isRefreshing) return;
    
    isRefreshing = true;
    
    try {
      console.log('🔄 Keepalive: refreshing session...');
      const response = await fetch('/api/auth/validate', {
        method: 'GET',
        credentials: 'same-origin'
      });
      
      if (response.ok) {
        const data = await response.json();
        
        if (data.status === 'refreshed') {
          console.log('✅ Session extended successfully');
          // Уведомляем компоненты о обновлении данных пользователя
          window.dispatchEvent(new CustomEvent('user-data-updated'));
        } else if (data.status === 'valid') {
          console.log('✅ Session still valid');
        }
      } else if (response.status === 401) {
        console.log('❌ Session expired, redirecting to login...');
        window.location.href = '/login';
      }
    } catch (error) {
      console.error('Keepalive error:', error);
    } finally {
      isRefreshing = false;
    }
  }
  
  onMount(() => {
    console.log('📌 Starting keepalive service');
    
    // Запускаем интервал
    refreshInterval = setInterval(refreshSession, REFRESH_INTERVAL);
    
    // Опционально: делаем первый запрос через 1 минуту после загрузки
    setTimeout(refreshSession, 60 * 1000);
    
    // Очищаем интервал при размонтировании
    return () => {
      if (refreshInterval) {
        clearInterval(refreshInterval);
      }
    };
  });
  
  onDestroy(() => {
    if (refreshInterval) {
      clearInterval(refreshInterval);
    }
  });
</script>

<svelte:head>
</svelte:head>

<Modal
    isOpen={$modal.isOpen}
    title={$modal.title}
    message={$modal.message}
    type={$modal.type}
    showConfirm={$modal.showConfirm}
    confirmText={$modal.confirmText}
    cancelText={$modal.cancelText}
    onConfirm={() => {
        $modal.onConfirm?.();
        modal.close();
    }}
    onCancel={() => {
        $modal.onCancel?.();
        modal.close();
    }}
    onClose={modal.close}
/>

{@render children()}