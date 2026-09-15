import { useQuery } from '@tanstack/react-query';
import { Bell } from 'lucide-react';
import { Link } from 'react-router-dom';
import { notificationApi } from '../../api/endpoints';
import { Badge, Button } from '../../components/ui';

export function NotificationBell() {
  const { data } = useQuery({ queryKey: ['notifications', 'unread-count'], queryFn: notificationApi.unreadCount });
  const count = data?.count ?? 0;
  return (
    <Button asChild variant="secondary">
      <Link to="/notifications" aria-label={`${count} unread notifications`}>
        <Bell size={16} />
        {count > 0 ? <Badge tone="info">{count}</Badge> : null}
      </Link>
    </Button>
  );
}
