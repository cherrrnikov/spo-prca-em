<script lang="ts">
  import Modal from '$lib/components/Modal.svelte';
  import Spinner from '$lib/components/Spinner.svelte';
  import { KEEPALIVE_INTERVAL_MS } from '$lib/config/api.config';
  import { modal } from '$lib/services/modal.service';
  import { onDestroy, onMount } from 'svelte';
  import '../app.css';
  
  let { children } = $props();
  let refreshInterval: ReturnType<typeof setInterval> | null = null;
  let isRefreshing = false;
  
  async function refreshSession() {
    if (isRefreshing) return;
    
    isRefreshing = true;
    
    try {
      const response = await fetch('/proxy/auth/validate', {
        method: 'GET',
        credentials: 'same-origin'
      });
      
      if (response.ok) {
        const data = await response.json();
        
        if (data.status === 'refreshed') {
          // Уведомляем компоненты о обновлении данных пользователя
          window.dispatchEvent(new CustomEvent('user-data-updated'));
        } else if (data.status === 'valid') {
        }
      } else if (response.status === 401) {
        window.location.href = '/login';
      }
    } catch (error) {
      console.error('Keepalive error:', error);
    } finally {
      isRefreshing = false;
    }
  }
  
  onMount(() => {
    
    // Запускаем интервал
    refreshInterval = setInterval(refreshSession, KEEPALIVE_INTERVAL_MS);
    
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

<Spinner />

{@render children()}